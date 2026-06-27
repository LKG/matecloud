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
package vip.mate.starter.distribute.worker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages the Snowflake worker ID for the current JVM instance.
 * <p>
 * The Snowflake worker ID is a 5-bit field, so at most {@value #SLOT_COUNT}
 * instances of a given {@code spring.application.name} can coexist with unique
 * IDs. On startup this holder <b>leases a free slot</b> in {@code [0, 31]} via
 * an atomic {@code SETNX} against Redis, keeps it alive with a heartbeat, and
 * releases it on shutdown — so a crashed instance's slot is reclaimed once its
 * lease expires.
 * <p>
 * This replaces the earlier {@code incrementAndGet() % 32} scheme, which handed
 * out duplicate worker IDs after 32 allocations/restarts (the counter only ever
 * grew and wrapped), causing Snowflake IDs to collide across instances.
 *
 * @author mateaix
 */
@Slf4j
@RequiredArgsConstructor
public class WorkerIdHolder implements CommandLineRunner {

    private static final String SLOT_KEY_PREFIX = "mate:distribute:worker:";
    /** Size of the Snowflake 5-bit worker ID space (worker IDs 0-31). */
    private static final int SLOT_COUNT = 32;
    /** Slot lease TTL; a crashed instance's slot frees up after this. */
    private static final Duration LEASE_TTL = Duration.ofSeconds(60);
    /** Renew well within the TTL so transient Redis hiccups don't drop the lease. */
    private static final long HEARTBEAT_SECONDS = 20;

    private final RedissonClient redissonClient;

    @Value("${spring.application.name:mate-default}")
    private String applicationName;

    /** Unique identity of this JVM, stored as the slot's value to detect ownership. */
    private final String instanceId = UUID.randomUUID().toString();

    private volatile String slotKey;
    private ScheduledExecutorService heartbeat;

    /**
     * The Snowflake worker ID for this JVM instance. Set once during startup.
     * {@code volatile} so request threads reliably observe the value written by
     * the startup {@code CommandLineRunner} thread (cross-thread visibility).
     */
    public static volatile long WORKER_ID;

    @Override
    public void run(String... args) {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            String key = SLOT_KEY_PREFIX + applicationName + ":slot:" + slot;
            RBucket<String> bucket = redissonClient.getBucket(key);
            if (bucket.setIfAbsent(instanceId, LEASE_TTL)) {
                WORKER_ID = slot;
                slotKey = key;
                startHeartbeat();
                log.info("[mate-distribute] Worker ID {} leased for application '{}' (instance {})",
                        slot, applicationName, instanceId);
                return;
            }
        }
        // All 32 slots are held — the cluster has exceeded the Snowflake worker-ID
        // capacity for this application. Failing loud beats silently issuing a
        // duplicate worker ID and corrupting every generated Snowflake ID.
        throw new IllegalStateException("[mate-distribute] No free Snowflake worker ID slot for application '"
                + applicationName + "': all " + SLOT_COUNT + " slots are in use. "
                + "The Snowflake 5-bit worker ID space caps a single application at " + SLOT_COUNT + " concurrent instances.");
    }

    /**
     * Periodically renews the slot lease, but only while we still own it — guards
     * against extending a slot that was reclaimed (e.g. after a long GC/Redis stall).
     */
    private void startHeartbeat() {
        heartbeat = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "mate-worker-id-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
        heartbeat.scheduleAtFixedRate(() -> {
            try {
                RBucket<String> bucket = redissonClient.getBucket(slotKey);
                if (instanceId.equals(bucket.get())) {
                    bucket.expire(LEASE_TTL);
                } else {
                    log.warn("[mate-distribute] Worker ID {} slot no longer owned by this instance ({}); "
                            + "lease was reclaimed.", WORKER_ID, instanceId);
                }
            } catch (Exception e) {
                log.warn("[mate-distribute] Failed to renew worker ID lease for slot {}: {}", WORKER_ID, e.getMessage());
            }
        }, HEARTBEAT_SECONDS, HEARTBEAT_SECONDS, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void release() {
        if (heartbeat != null) {
            heartbeat.shutdownNow();
        }
        if (slotKey == null) {
            return;
        }
        try {
            RBucket<String> bucket = redissonClient.getBucket(slotKey);
            // Only delete the slot if we still own it (compare-and-delete semantics).
            if (instanceId.equals(bucket.get())) {
                bucket.delete();
                log.info("[mate-distribute] Released worker ID {} for application '{}'", WORKER_ID, applicationName);
            }
        } catch (Exception e) {
            log.warn("[mate-distribute] Failed to release worker ID slot {}: {}", WORKER_ID, e.getMessage());
        }
    }
}
