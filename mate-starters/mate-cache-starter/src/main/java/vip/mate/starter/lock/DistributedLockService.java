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
package vip.mate.starter.lock;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Programmatic distributed lock API.
 *
 * @author mateaix
 */
public class DistributedLockService {

    private static final Logger log = LoggerFactory.getLogger(DistributedLockService.class);

    private static final String LOCK_KEY_PREFIX = "mate:lock:";

    private final RedissonClient redissonClient;

    public DistributedLockService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public <T> T executeWithLock(String lockName, long waitTime, long leaseTime,
                                 TimeUnit timeUnit, Supplier<T> supplier) {
        return executeWithLock(lockName, LockType.REENTRANT, waitTime, leaseTime, timeUnit, supplier);
    }

    public <T> T executeWithLock(String lockName, LockType lockType, long waitTime, long leaseTime,
                                 TimeUnit timeUnit, Supplier<T> supplier) {
        String key = LOCK_KEY_PREFIX + lockName;
        RLock lock = lockType.getLock(redissonClient, key);
        boolean acquired = false;

        try {
            // leaseTime <= 0 -> use Redisson's watchdog (auto-renews the lease
            // while the thread holds it) so a slow critical section can't have its
            // lock expire out from under it and let a second holder run concurrently.
            acquired = leaseTime <= 0
                    ? lock.tryLock(waitTime, timeUnit)
                    : lock.tryLock(waitTime, leaseTime, timeUnit);
            if (!acquired) {
                throw new DistributedLockException(
                        "Failed to acquire distributed lock [" + key
                                + "] within " + waitTime + " " + timeUnit);
            }
            log.debug("Acquired distributed lock [{}]", key);
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DistributedLockException("Lock acquisition interrupted for [" + key + "]", e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Released distributed lock [{}]", key);
            }
        }
    }

    public void executeWithLock(String lockName, long waitTime, long leaseTime,
                                TimeUnit timeUnit, Runnable runnable) {
        executeWithLock(lockName, waitTime, leaseTime, timeUnit, () -> {
            runnable.run();
            return null;
        });
    }

    public <T> T executeWithLock(String lockName, Supplier<T> supplier) {
        return executeWithLock(lockName, 3, -1, TimeUnit.SECONDS, supplier);
    }

    public void executeWithLock(String lockName, Runnable runnable) {
        executeWithLock(lockName, 3, -1, TimeUnit.SECONDS, runnable);
    }

    public RLock tryLock(String lockName, long waitTime, long leaseTime, TimeUnit timeUnit) {
        return tryLock(lockName, LockType.REENTRANT, waitTime, leaseTime, timeUnit);
    }

    public RLock tryLock(String lockName, LockType lockType, long waitTime, long leaseTime, TimeUnit timeUnit) {
        String key = LOCK_KEY_PREFIX + lockName;
        RLock lock = lockType.getLock(redissonClient, key);
        try {
            boolean acquired = leaseTime <= 0
                    ? lock.tryLock(waitTime, timeUnit)
                    : lock.tryLock(waitTime, leaseTime, timeUnit);
            if (!acquired) {
                return null;
            }
            return lock;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DistributedLockException("Lock acquisition interrupted for [" + key + "]", e);
        }
    }
}
