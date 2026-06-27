/*
 * Copyright (c) 2024-2026 Beijing Daotiandi Technology Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package vip.mate.system.admin.application.query.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;
import vip.mate.system.admin.application.query.IMonitorQueryService;
import vip.mate.system.admin.infrastructure.dao.AdminDao;
import vip.mate.system.admin.infrastructure.dao.LoginLogDao;
import vip.mate.system.admin.infrastructure.dao.OperationLogDao;
import vip.mate.system.admin.infrastructure.dao.po.AdminPO;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Aggregates Dashboard data from three sources:
 * <ul>
 *   <li><b>SQL</b> — admin count, today's login/op counts, last-7-days bucket</li>
 *   <li><b>Sa-Token Redis</b> — count of currently active session tokens</li>
 *   <li><b>HTTP probe</b> — concurrent {@code GET /actuator/health} on every
 *       known backend service so the Services panel can show UP/DOWN dots.</li>
 * </ul>
 *
 * <p>Each source is best-effort: a Redis hiccup, a missing Sa-Token namespace,
 * or one downed actuator endpoint can not crash the dashboard call. Failures
 * collapse to zero or {@code "DOWN"} so the UI keeps rendering.
 *
 * @author mateaix
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitorQueryServiceImpl implements IMonitorQueryService {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final SimpleDateFormat ISO_DAY = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);

    private final AdminDao adminDao;
    private final LoginLogDao loginLogDao;
    private final OperationLogDao operationLogDao;

    /**
     * Optional Spring Cloud discovery client (Nacos). When present (the normal
     * case), service health is driven by what's actually registered in Nacos —
     * no hard-coded list, no stale entries (e.g. the merged-away mate-admin).
     * When the discovery client isn't on the classpath, falls back to the
     * {@link #servicesConfig} static list.
     */
    @Autowired(required = false)
    private DiscoveryClient discoveryClient;

    /**
     * Optional override list of {@code name=baseUrl} pairs. Leave empty (the
     * default) to use Nacos discovery; set explicitly to monitor an external
     * service that isn't registered. Example:
     *
     * <pre>
     * mate.monitor.services: legacy=http://10.0.0.10:8080,extranet=https://api.example.com
     * </pre>
     */
    @Value("${mate.monitor.services:}")
    private String servicesConfig;

    /**
     * Service names to hide from the workbench's health panel. Empty by default
     * so every registered {@code mate-*} app shows up — operators expect the
     * panel to reflect what's actually running. Comma-separated; matched against
     * the full registered name. Dubbo provider names ({@code *-dubbo}) are always
     * filtered separately.
     */
    @Value("${mate.monitor.exclude:}")
    private String excludeConfig;

    /**
     * Optional Redisson (mate-cache-starter). The gateway increments a per-day
     * {@code mate:metrics:api:yyyy-MM-dd} counter for every {@code /api/**}
     * request; we read the last 7 keys back for the real "API 调用" trend.
     * Shared Redis db 0, so the keys the gateway writes are visible here.
     */
    @Autowired(required = false)
    private RedissonClient redisson;

    /** Mirror of {@code ApiCallMetricsFilter.KEY_PREFIX} on the gateway side. */
    private static final String API_METRIC_PREFIX = "mate:metrics:api:";

    @Override
    public DashboardVO dashboard() {
        LocalDateTime startOfToday = LocalDate.now(ZONE).atStartOfDay();
        LocalDateTime sevenDaysAgo = LocalDate.now(ZONE).minusDays(6).atStartOfDay();

        long adminCount = countAdmins();
        long todayLogin = safeCount(() -> loginLogDao.countSince(startOfToday));
        long todayOp = safeCount(() -> operationLogDao.countSince(startOfToday));
        long online = countOnlineSessions();

        return new DashboardVO(
                adminCount,
                todayLogin,
                todayOp,
                online,
                buildDailyStats(sevenDaysAgo),
                probeServices()
        );
    }

    // ---- counts ----

    private long countAdmins() {
        try {
            // Logical-delete column is honored automatically by @TableLogic; this
            // pulls only live rows. Cast to long is safe, MyBatis-Plus returns Long.
            return adminDao.selectCount(new LambdaQueryWrapper<AdminPO>());
        } catch (Exception e) {
            log.warn("Admin count failed: {}", e.getMessage());
            return 0L;
        }
    }

    private long safeCount(LongSupplier fn) {
        try { return fn.getAsLong(); }
        catch (Exception e) {
            log.warn("Stat count failed: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * Counts currently-active Sa-Token sessions by scanning all token keys.
     * Sa-Token's {@code searchTokenValue} is a thin wrapper over Redis SCAN
     * — fine for a few hundred tokens, would need a separate counter cache
     * if the system grows past tens of thousands of concurrent sessions.
     */
    private long countOnlineSessions() {
        try {
            List<String> tokens = StpUtil.searchTokenValue("", 0, -1, false);
            return tokens == null ? 0L : tokens.size();
        } catch (Exception e) {
            log.warn("Online session count failed: {}", e.getMessage());
            return 0L;
        }
    }

    // ---- 7-day trend ----

    /**
     * Build a 7-row series ending today. Ensures every day has an entry even
     * when there's no data for that date — the frontend chart needs a dense
     * series so bars line up with x-axis labels.
     *
     * <p>{@code apiCalls} is the real per-day gateway request count (written by
     * {@code ApiCallMetricsFilter} into Redis); {@code logins} comes from the
     * login-log table. Previously {@code apiCalls} read the operation-log table
     * — only audited admin mutations — so it was near-empty and mislabeled.
     */
    private List<DailyStat> buildDailyStats(LocalDateTime since) {
        Map<String, Long> loginByDay = bucketize(safeBuckets(() -> loginLogDao.countByDateSince(since)));

        List<DailyStat> series = new ArrayList<>(7);
        LocalDate today = LocalDate.now(ZONE);
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            String key = day.toString(); // yyyy-MM-dd
            series.add(new DailyStat(
                    key,
                    apiCallsForDay(day),
                    loginByDay.getOrDefault(key, 0L)));
        }
        return series;
    }

    /**
     * Read one day's gateway API-call count from Redis. Returns 0 when Redisson
     * isn't on the classpath or the key is absent (e.g. a day with no traffic,
     * or before this counter shipped) — metrics never fail the dashboard.
     */
    private long apiCallsForDay(LocalDate day) {
        if (redisson == null) return 0L;
        try {
            return redisson.getAtomicLong(API_METRIC_PREFIX + day).get();
        } catch (Exception e) {
            log.warn("API-call metric read failed for {}: {}", day, e.getMessage());
            return 0L;
        }
    }

    private List<Map<String, Object>> safeBuckets(
            Supplier<List<Map<String, Object>>> fn) {
        try { return fn.get(); }
        catch (Exception e) {
            log.warn("Bucket query failed: {}", e.getMessage());
            return List.of();
        }
    }

    /** Convert MyBatis raw rows {@code [{date, cnt}]} into a {@code Map<isoDate, count>}. */
    private Map<String, Long> bucketize(List<Map<String, Object>> rows) {
        Map<String, Long> out = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object rawDate = row.get("date");
            String key = rawDate instanceof Date d ? ISO_DAY.format(d) : String.valueOf(rawDate);
            Object cnt = row.get("cnt");
            out.put(key, cnt instanceof Number n ? n.longValue() : 0L);
        }
        return out;
    }

    // ---- service health probes ----

    /**
     * Fan out a 1.5-second {@code GET /actuator/health} per discovered service
     * and zip latencies back into a {@code List<ServiceStatus>}.
     *
     * <p>Source of services (in priority order):
     * <ol>
     *   <li>{@link #servicesConfig} if explicitly set — for external/legacy targets;</li>
     *   <li>otherwise the {@link DiscoveryClient} (Nacos registry) —
     *       what's actually registered, with the actual host:port from
     *       each instance's metadata. Filters to {@code mate-*} services and
     *       excludes Dubbo provider names ({@code *-dubbo}).</li>
     * </ol>
     */
    private List<ServiceStatus> probeServices() {
        List<String[]> services = parseServices(servicesConfig);
        if (services.isEmpty()) {
            services = discoverServices();
        }
        List<CompletableFuture<ServiceStatus>> futures = services.stream()
                .map(s -> probeOne(s[0], s[1]))
                .toList();
        List<ServiceStatus> results = new ArrayList<>(futures.size());
        for (CompletableFuture<ServiceStatus> f : futures) {
            try {
                results.add(f.get(2, TimeUnit.SECONDS));
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                Thread.currentThread().interrupt();
                results.add(new ServiceStatus("?", "?", "DOWN", null));
            }
        }
        return results;
    }

    /**
     * Discover the platform services from Nacos: filter to {@code mate-*},
     * drop Dubbo provider entries ({@code *-dubbo}), then resolve each to its
     * first instance's {@code http://host:port} base URL. The display name
     * strips the {@code mate-} prefix to match the original UI labels
     * (gateway / auth / system / notice).
     */
    private List<String[]> discoverServices() {
        if (discoveryClient == null) {
            log.warn("[monitor] DiscoveryClient unavailable — services panel will be empty");
            return List.of();
        }
        Set<String> excluded = excludedServices();
        try {
            return discoveryClient.getServices().stream()
                    .filter(s -> s != null && s.startsWith("mate-") && !s.endsWith("-dubbo"))
                    .filter(s -> !excluded.contains(s))
                    .sorted()
                    .map(serviceId -> {
                        List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
                        if (instances == null || instances.isEmpty()) {
                            return new String[]{stripPrefix(serviceId), ""}; // empty url → probe DOWN
                        }
                        ServiceInstance inst = instances.get(0);
                        String url = "http://" + inst.getHost() + ":" + inst.getPort();
                        return new String[]{stripPrefix(serviceId), url};
                    })
                    .toList();
        } catch (Exception e) {
            log.warn("[monitor] Service discovery failed: {}", e.getMessage());
            return List.of();
        }
    }

    private static String stripPrefix(String serviceId) {
        return serviceId.startsWith("mate-") ? serviceId.substring(5) : serviceId;
    }

    /** Parse the comma-separated {@code mate.monitor.exclude} list into a set. */
    private Set<String> excludedServices() {
        Set<String> out = new HashSet<>();
        if (excludeConfig != null && !excludeConfig.isBlank()) {
            for (String name : excludeConfig.split(",")) {
                String trimmed = name.trim();
                if (!trimmed.isEmpty()) out.add(trimmed);
            }
        }
        return out;
    }

    private List<String[]> parseServices(String cfg) {
        List<String[]> out = new ArrayList<>();
        if (cfg == null || cfg.isBlank()) return out;
        for (String pair : cfg.split(",")) {
            int eq = pair.indexOf('=');
            if (eq <= 0 || eq == pair.length() - 1) continue;
            String name = pair.substring(0, eq).trim();
            String url = pair.substring(eq + 1).trim();
            out.add(new String[]{name, url});
        }
        return out;
    }

    private CompletableFuture<ServiceStatus> probeOne(String name, String baseUrl) {
        long started = System.currentTimeMillis();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/actuator/health"))
                .timeout(Duration.ofMillis(1500))
                .GET()
                .build();
        return HTTP.sendAsync(req, HttpResponse.BodyHandlers.discarding())
                .handle((resp, ex) -> {
                    long latency = System.currentTimeMillis() - started;
                    if (ex != null || resp == null) {
                        // Actuator off or service down — both render as DOWN to the operator.
                        return new ServiceStatus(name, baseUrl, "DOWN", latency);
                    }
                    String state = resp.statusCode() == 200 ? "UP" : "DOWN";
                    return new ServiceStatus(name, baseUrl, state, latency);
                });
    }
}
