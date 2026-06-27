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
package vip.mate.starter.sso.core.redis;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import vip.mate.base.exception.BizException;
import vip.mate.starter.sso.core.sync.SyncLock;
import vip.mate.starter.sso.types.SsoErrorCode;

/**
 * Clustered {@link SyncLock} on Redisson — replaces PlayEdu's
 * {@code isSynchronizeing()} spin flag with a real distributed mutex. No wait
 * (a concurrent sync for the same provider is rejected fast); watchdog lease so
 * the lock can't expire mid-run. Auto-selected when {@code RedissonClient} exists.
 *
 * @author mateaix
 */
public class RedissonSyncLock implements SyncLock {

    private final RedissonClient redisson;

    public RedissonSyncLock(RedissonClient redisson) {
        this.redisson = redisson;
    }

    @Override
    public <T> T runExclusive(String key, Supplier<T> action) {
        RLock lock = redisson.getLock(key);
        boolean acquired;
        try {
            acquired = lock.tryLock(0, -1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(SsoErrorCode.SSO_B_SYNC_RUNNING.getCode(), "同步锁获取被中断");
        }
        if (!acquired) {
            throw new BizException(SsoErrorCode.SSO_B_SYNC_RUNNING.getCode(),
                    SsoErrorCode.SSO_B_SYNC_RUNNING.getMessage());
        }
        try {
            return action.get();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
