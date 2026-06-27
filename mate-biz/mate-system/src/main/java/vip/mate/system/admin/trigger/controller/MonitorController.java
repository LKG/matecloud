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
package vip.mate.system.admin.trigger.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import vip.mate.base.result.PageResult;
import vip.mate.base.result.Result;
import vip.mate.system.admin.application.query.IMonitorQueryService;
import vip.mate.system.admin.application.query.IMonitorQueryService.DashboardVO;
import vip.mate.system.admin.types.security.Perms;

import java.io.File;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Read-only endpoints powering the Dashboard / Server Status / Cache Monitor views.
 *
 * <p>Only {@code @SaCheckLogin} — every authenticated user can see these
 * metrics. Per-card permission gating happens on the frontend.
 *
 * @author mateaix
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/monitor")
@RequiredArgsConstructor
@SaCheckLogin
public class MonitorController {

    private final IMonitorQueryService monitorQueryService;
    private final StringRedisTemplate stringRedisTemplate;

    /** Hard cap on SCAN results to prevent OOM on huge Redis instances. */
    private static final int MAX_KEY_SCAN = 5000;
    /** Max value preview length (bytes) to avoid transferring huge blobs. */
    private static final int MAX_VALUE_PREVIEW = 4096;

    @GetMapping("/dashboard")
    public Result<DashboardVO> dashboard() {
        return Result.ok(monitorQueryService.dashboard());
    }

    /**
     * Server runtime information: CPU, JVM, OS, and disk.
     */
    @GetMapping("/server")
    public Result<Map<String, Object>> serverInfo() {
        Map<String, Object> data = new LinkedHashMap<>();

        Runtime rt = Runtime.getRuntime();

        // ---- CPU info (via OperatingSystemMXBean) ----
        Map<String, Object> cpu = new LinkedHashMap<>();
        cpu.put("cores", rt.availableProcessors());
        try {
            OperatingSystemMXBean osMx =
                    ManagementFactory.getOperatingSystemMXBean();
            double loadAvg = osMx.getSystemLoadAverage();
            cpu.put("systemLoadAverage", loadAvg >= 0 ? String.format("%.2f", loadAvg) : "N/A");
            // Try to get process CPU usage via com.sun.management (available on HotSpot)
            if (osMx instanceof com.sun.management.OperatingSystemMXBean sunMx) {
                double cpuLoad = sunMx.getProcessCpuLoad();
                cpu.put("processUsage", cpuLoad >= 0 ? String.format("%.1f%%", cpuLoad * 100) : "N/A");
                double sysCpu = sunMx.getCpuLoad();
                cpu.put("systemUsage", sysCpu >= 0 ? String.format("%.1f%%", sysCpu * 100) : "N/A");
                long totalPhysical = sunMx.getTotalMemorySize();
                // getFreeMemorySize() on macOS only returns "free" pages, excluding
                // inactive/purgeable cache that the OS can reclaim instantly.
                // Use OS-specific commands for accurate "available" memory.
                long availableMemory = getAvailableMemory(totalPhysical, sunMx.getFreeMemorySize());
                long usedMemory = totalPhysical - availableMemory;
                cpu.put("totalPhysicalMemory", totalPhysical / 1024 / 1024 + " MB");
                cpu.put("freePhysicalMemory", availableMemory / 1024 / 1024 + " MB");
                cpu.put("usedPhysicalMemory", usedMemory / 1024 / 1024 + " MB");
            }
        } catch (Exception e) {
            cpu.put("systemLoadAverage", "N/A");
            cpu.put("processUsage", "N/A");
            cpu.put("systemUsage", "N/A");
        }
        data.put("cpu", cpu);

        // ---- JVM info ----
        Map<String, Object> jvm = new LinkedHashMap<>();
        long maxMem = rt.maxMemory();
        long totalMem = rt.totalMemory();
        long freeMem = rt.freeMemory();
        long usedMem = totalMem - freeMem;
        jvm.put("maxMemory", maxMem / 1024 / 1024 + " MB");
        jvm.put("totalMemory", totalMem / 1024 / 1024 + " MB");
        jvm.put("freeMemory", freeMem / 1024 / 1024 + " MB");
        jvm.put("usedMemory", usedMem / 1024 / 1024 + " MB");
        jvm.put("usagePercent", maxMem > 0 ? String.format("%.1f%%", usedMem * 100.0 / maxMem) : "N/A");
        jvm.put("javaVersion", System.getProperty("java.version"));
        jvm.put("jvmName", System.getProperty("java.vm.name"));
        jvm.put("javaHome", System.getProperty("java.home"));
        // JVM uptime
        RuntimeMXBean runtimeMx =
                ManagementFactory.getRuntimeMXBean();
        long uptimeMs = runtimeMx.getUptime();
        jvm.put("startTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new Date(runtimeMx.getStartTime())));
        jvm.put("uptime", formatUptime(uptimeMs));
        // GC info
        long gcCount = 0, gcTime = 0;
        for (GarbageCollectorMXBean gc :
                ManagementFactory.getGarbageCollectorMXBeans()) {
            gcCount += gc.getCollectionCount();
            gcTime += gc.getCollectionTime();
        }
        jvm.put("gcCount", gcCount);
        jvm.put("gcTime", gcTime + " ms");
        // Thread count
        jvm.put("threadCount", ManagementFactory.getThreadMXBean().getThreadCount());
        data.put("jvm", jvm);

        // ---- OS info ----
        Map<String, Object> os = new LinkedHashMap<>();
        os.put("name", System.getProperty("os.name"));
        os.put("arch", System.getProperty("os.arch"));
        os.put("version", System.getProperty("os.version"));
        os.put("availableProcessors", rt.availableProcessors());
        os.put("userDir", System.getProperty("user.dir"));
        data.put("os", os);

        // ---- Disk info (cross-platform: use working directory's file store) ----
        Map<String, Object> disk = new LinkedHashMap<>();
        File root = new File(System.getProperty("user.dir"));
        // Walk up to the filesystem root to get total disk info
        while (root.getParentFile() != null) {
            root = root.getParentFile();
        }
        long diskTotal = root.getTotalSpace();
        long diskFree = root.getFreeSpace();
        long diskUsed = diskTotal - diskFree;
        disk.put("total", diskTotal / 1024 / 1024 / 1024 + " GB");
        disk.put("free", diskFree / 1024 / 1024 / 1024 + " GB");
        disk.put("used", diskUsed / 1024 / 1024 / 1024 + " GB");
        disk.put("usagePercent", diskTotal > 0 ? String.format("%.1f%%", diskUsed * 100.0 / diskTotal) : "N/A");
        data.put("disk", disk);

        return Result.ok(data);
    }

    private String formatUptime(long ms) {
        long seconds = ms / 1000;
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        if (days > 0) return days + "d " + hours + "h " + minutes + "m";
        if (hours > 0) return hours + "h " + minutes + "m";
        return minutes + "m";
    }

    /**
     * Redis cache information: version, memory, connected clients, uptime, dbSize,
     * and command statistics (for pie chart).
     */
    @GetMapping("/cache")
    public Result<Map<String, Object>> cacheInfo() {
        Map<String, Object> data = new LinkedHashMap<>();

        RedisConnection connection = null;
        try {
            connection = stringRedisTemplate.getConnectionFactory().getConnection();
            Properties info = connection.serverCommands().info();
            Properties commandStats = connection.serverCommands().info("commandstats");

            // ---- basic info ----
            Map<String, Object> redis = new LinkedHashMap<>();
            redis.put("version", info.getProperty("redis_version"));
            redis.put("mode", info.getProperty("redis_mode", "standalone"));
            redis.put("os", info.getProperty("os"));
            redis.put("tcpPort", info.getProperty("tcp_port"));
            redis.put("usedMemory", info.getProperty("used_memory_human"));
            redis.put("usedMemoryRss", info.getProperty("used_memory_rss_human"));
            redis.put("usedMemoryPeak", info.getProperty("used_memory_peak_human"));
            redis.put("maxMemory", info.getProperty("maxmemory_human", "unlimited"));
            redis.put("maxMemoryPolicy", info.getProperty("maxmemory_policy", "noeviction"));
            redis.put("connectedClients", info.getProperty("connected_clients"));
            redis.put("blockedClients", info.getProperty("blocked_clients"));
            redis.put("uptimeDays", info.getProperty("uptime_in_days"));
            redis.put("uptimeSeconds", info.getProperty("uptime_in_seconds"));
            redis.put("dbSize", connection.serverCommands().dbSize());
            redis.put("totalConnectionsReceived", info.getProperty("total_connections_received"));
            redis.put("totalCommandsProcessed", info.getProperty("total_commands_processed"));
            redis.put("instantaneousOpsPerSec", info.getProperty("instantaneous_ops_per_sec"));
            redis.put("keyspaceHits", info.getProperty("keyspace_hits"));
            redis.put("keyspaceMisses", info.getProperty("keyspace_misses"));
            // Hit rate calculation
            long hits = parseLong(info.getProperty("keyspace_hits"));
            long misses = parseLong(info.getProperty("keyspace_misses"));
            redis.put("hitRate", (hits + misses) > 0
                    ? String.format("%.2f%%", hits * 100.0 / (hits + misses)) : "N/A");
            redis.put("usedCpuSys", info.getProperty("used_cpu_sys"));
            redis.put("usedCpuUser", info.getProperty("used_cpu_user"));
            data.put("redis", redis);

            // ---- command stats for pie chart ----
            List<Map<String, String>> cmdStatsList = new ArrayList<>();
            if (commandStats != null) {
                commandStats.stringPropertyNames().forEach(key -> {
                    Map<String, String> item = new LinkedHashMap<>();
                    String property = commandStats.getProperty(key);
                    item.put("name", key.replace("cmdstat_", ""));
                    // Extract calls count: "calls=123,usec=456,usec_per_call=3.71"
                    String calls = extractBetween(property, "calls=", ",");
                    item.put("value", calls != null ? calls : "0");
                    cmdStatsList.add(item);
                });
                // Sort by calls descending, keep top 12
                cmdStatsList.sort((a, b) ->
                        Long.compare(parseLong(b.get("value")), parseLong(a.get("value"))));
                data.put("commandStats", cmdStatsList.size() > 12
                        ? cmdStatsList.subList(0, 12) : cmdStatsList);
            } else {
                data.put("commandStats", cmdStatsList);
            }

        } catch (Exception e) {
            log.warn("Failed to fetch Redis info: {}", e.getMessage());
            Map<String, Object> redis = new LinkedHashMap<>();
            redis.put("version", "N/A");
            redis.put("usedMemory", "N/A");
            redis.put("maxMemory", "N/A");
            redis.put("connectedClients", "N/A");
            redis.put("uptimeDays", "N/A");
            redis.put("dbSize", 0);
            redis.put("hitRate", "N/A");
            data.put("redis", redis);
            data.put("commandStats", List.of());
        } finally {
            if (connection != null) {
                try { connection.close(); } catch (Exception ignored) {}
            }
        }

        return Result.ok(data);
    }

    /**
     * Scan Redis keys with optional pattern filter.
     * Uses SCAN (non-blocking) instead of KEYS to avoid blocking Redis.
     */
    @GetMapping("/cache/keys")
    public Result<PageResult<Map<String, Object>>> cacheKeys(
            @RequestParam(defaultValue = "*") String pattern,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {

        List<String> allKeys = new ArrayList<>();
        try {
            RedisConnection connection = stringRedisTemplate.getConnectionFactory().getConnection();
            try {
                ScanOptions options = ScanOptions.scanOptions()
                        .match(pattern)
                        .count(200)
                        .build();
                Cursor<byte[]> cursor = connection.keyCommands().scan(options);
                while (cursor.hasNext() && allKeys.size() < MAX_KEY_SCAN) {
                    allKeys.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
                cursor.close();
            } finally {
                try { connection.close(); } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            log.warn("Failed to scan Redis keys: {}", e.getMessage());
            return Result.ok(PageResult.of(List.of(), 0L));
        }

        // Sort alphabetically for stable pagination
        allKeys.sort(String::compareTo);

        long total = allKeys.size();
        int from = Math.min((pageNum - 1) * pageSize, allKeys.size());
        int to = Math.min(from + pageSize, allKeys.size());
        List<String> pageKeys = allKeys.subList(from, to);

        // Enrich each key with type + TTL
        List<Map<String, Object>> rows = new ArrayList<>(pageKeys.size());
        for (String key : pageKeys) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("key", key);
            try {
                DataType type = stringRedisTemplate.type(key);
                row.put("type", type != null ? type.code() : "unknown");
                Long ttl = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
                row.put("ttl", ttl != null ? ttl : -2);
                // Approximate size via MEMORY USAGE (Redis 4.0+)
                try {
                    Long memUsage = stringRedisTemplate.execute((RedisConnection conn) -> {
                        Object result = conn.execute("MEMORY",
                                "USAGE".getBytes(StandardCharsets.UTF_8),
                                key.getBytes(StandardCharsets.UTF_8));
                        if (result instanceof Long) return (Long) result;
                        // Some drivers return Integer
                        if (result instanceof Number) return ((Number) result).longValue();
                        return null;
                    });
                    row.put("size", memUsage != null ? formatBytes(memUsage) : "-");
                } catch (Exception e) {
                    log.debug("MEMORY USAGE failed for key {}: {}", key, e.getMessage());
                    row.put("size", "-");
                }
            } catch (Exception e) {
                row.put("type", "unknown");
                row.put("ttl", -2);
                row.put("size", "-");
            }
            rows.add(row);
        }

        return Result.ok(PageResult.of(rows, total));
    }

    /**
     * Get the value of a specific Redis key. Supports all Redis data types.
     * Value is truncated to {@link #MAX_VALUE_PREVIEW} chars.
     */
    @GetMapping("/cache/keys/{key}")
    public Result<Map<String, Object>> cacheKeyDetail(@PathVariable String key) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("key", key);

        DataType type = stringRedisTemplate.type(key);
        if (type == null || type == DataType.NONE) {
            return Result.fail("Key not found");
        }
        detail.put("type", type.code());
        Long ttl = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
        detail.put("ttl", ttl != null ? ttl : -2);

        // Memory usage
        try {
            Long memUsage = stringRedisTemplate.execute((RedisConnection conn) -> {
                Object result = conn.execute("MEMORY",
                        "USAGE".getBytes(StandardCharsets.UTF_8),
                        key.getBytes(StandardCharsets.UTF_8));
                if (result instanceof Long) return (Long) result;
                if (result instanceof Number) return ((Number) result).longValue();
                return null;
            });
            detail.put("size", memUsage != null ? formatBytes(memUsage) : "-");
        } catch (Exception e) {
            detail.put("size", "-");
        }

        // Read value based on type
        String value;
        try {
            value = switch (type) {
                case STRING -> stringRedisTemplate.opsForValue().get(key);
                case HASH -> {
                    Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
                    yield mapToJson(entries);
                }
                case LIST -> {
                    List<String> list = stringRedisTemplate.opsForList().range(key, 0, 99);
                    yield list != null ? list.toString() : "[]";
                }
                case SET -> {
                    Set<String> members = stringRedisTemplate.opsForSet().members(key);
                    yield members != null ? members.toString() : "[]";
                }
                case ZSET -> {
                    Set<String> zMembers = stringRedisTemplate.opsForZSet().range(key, 0, 99);
                    yield zMembers != null ? zMembers.toString() : "[]";
                }
                default -> "(unsupported type)";
            };
        } catch (Exception e) {
            value = "(error reading value: " + e.getMessage() + ")";
        }

        if (value != null && value.length() > MAX_VALUE_PREVIEW) {
            value = value.substring(0, MAX_VALUE_PREVIEW) + "... (truncated)";
        }
        detail.put("value", value);

        return Result.ok(detail);
    }

    /**
     * Delete a specific Redis key.
     */
    @DeleteMapping("/cache/keys/{key}")
    @SaCheckPermission(Perms.LOG_DELETE)
    public Result<Void> deleteCacheKey(@PathVariable String key) {
        Boolean deleted = stringRedisTemplate.delete(key);
        if (Boolean.TRUE.equals(deleted)) {
            return Result.ok();
        }
        return Result.fail("Key not found or already deleted");
    }

    // ---- helpers ----

    /**
     * Get accurate available memory. JVM's getFreeMemorySize() only reports truly
     * "free" pages on macOS/Linux, ignoring reclaimable cache (inactive/purgeable).
     * <ul>
     *   <li>Linux: reads MemAvailable from /proc/meminfo</li>
     *   <li>macOS: runs {@code vm_stat} and sums free + inactive + purgeable pages</li>
     *   <li>Fallback: returns the JVM-reported free memory</li>
     * </ul>
     */
    private long getAvailableMemory(long totalPhysical, long jvmFree) {
        String osName = System.getProperty("os.name", "").toLowerCase();
        try {
            if (osName.contains("linux")) {
                // Linux: read MemAvailable from /proc/meminfo (includes reclaimable cache)
                Path meminfo = Path.of("/proc/meminfo");
                if (Files.exists(meminfo)) {
                    for (String line : Files.readAllLines(meminfo)) {
                        if (line.startsWith("MemAvailable:")) {
                            String[] parts = line.split("\\s+");
                            return Long.parseLong(parts[1]) * 1024; // kB → bytes
                        }
                    }
                }
            } else if (osName.contains("mac")) {
                // macOS: use vm_stat to get free + inactive + purgeable pages
                Process proc = new ProcessBuilder("vm_stat").start();
                String output = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                proc.waitFor(3, TimeUnit.SECONDS);

                long pageSize = 16384; // Apple Silicon default
                // Parse page size from first line: "Mach Virtual Memory Statistics: (page size of 16384 bytes)"
                Matcher psm = Pattern
                        .compile("page size of (\\d+)").matcher(output);
                if (psm.find()) {
                    pageSize = Long.parseLong(psm.group(1));
                }

                long free = 0, inactive = 0, purgeable = 0, speculative = 0;
                for (String line : output.split("\n")) {
                    if (line.startsWith("Pages free:"))
                        free = parseVmStatValue(line);
                    else if (line.startsWith("Pages inactive:"))
                        inactive = parseVmStatValue(line);
                    else if (line.startsWith("Pages purgeable:"))
                        purgeable = parseVmStatValue(line);
                    else if (line.startsWith("Pages speculative:"))
                        speculative = parseVmStatValue(line);
                }
                long available = (free + inactive + purgeable + speculative) * pageSize;
                return available > 0 ? available : jvmFree;
            }
        } catch (Exception e) {
            log.debug("Failed to get OS available memory, falling back to JVM value: {}", e.getMessage());
        }
        return jvmFree;
    }

    private long parseVmStatValue(String line) {
        // Format: "Pages free:                             1234."
        Matcher m = Pattern.compile("(\\d+)").matcher(
                line.substring(line.indexOf(':') + 1));
        return m.find() ? Long.parseLong(m.group(1)) : 0;
    }

    private long parseLong(String value) {
        try { return value != null ? Long.parseLong(value.trim()) : 0; }
        catch (NumberFormatException e) { return 0; }
    }

    private String extractBetween(String str, String start, String end) {
        if (str == null) return null;
        int s = str.indexOf(start);
        if (s < 0) return null;
        s += start.length();
        int e = str.indexOf(end, s);
        return e > s ? str.substring(s, e) : str.substring(s);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / 1024.0 / 1024.0);
    }

    private String mapToJson(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (!first) sb.append(", ");
            sb.append("\"").append(escapeJson(String.valueOf(e.getKey())))
              .append("\": \"").append(escapeJson(String.valueOf(e.getValue()))).append("\"");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
