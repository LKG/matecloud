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
package vip.mate.starter.sso.core.sync;

import java.util.function.Supplier;

/**
 * Mutex around one provider's sync run (replaces PlayEdu's
 * {@code isSynchronizeing()} spin flag). Default impl {@link NoOp} is a no-op for
 * single-node / evaluation; override with a Redisson-based bean
 * (mate-cache-starter {@code @DistributedLock} / RedissonService) for a cluster.
 *
 * @author mateaix
 */
public interface SyncLock {

    /** Run {@code action} while holding the lock for {@code key}; skip if held. */
    <T> T runExclusive(String key, Supplier<T> action);

    /** Process-local no-op (no real mutex). */
    class NoOp implements SyncLock {
        @Override
        public <T> T runExclusive(String key, Supplier<T> action) {
            return action.get();
        }
    }
}
