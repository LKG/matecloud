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
package vip.mate.auth.infrastructure.adapter.cache;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import vip.mate.auth.domain.adapter.port.LoginAttemptPort;
import vip.mate.base.exception.BizException;
import vip.mate.base.response.ResponseCode;

import java.time.Duration;

/**
 * Redis-backed failure counter. After {@value #MAX_FAILURES} consecutive
 * failures the account is locked for {@value #LOCK_MINUTES} minutes.
 *
 * @author mateaix
 */
@Component
@RequiredArgsConstructor
public class RedisLoginAttemptCache implements LoginAttemptPort {

    private static final String KEY_PREFIX = "mate:auth:fail:";
    private static final int MAX_FAILURES = 5;
    private static final int LOCK_MINUTES = 30;

    private final RedissonClient redissonClient;

    @Override
    public void ensureNotLocked(String account) {
        RAtomicLong counter = redissonClient.getAtomicLong(KEY_PREFIX + account);
        if (counter.get() >= MAX_FAILURES) {
            throw new BizException(ResponseCode.FORBIDDEN.getCode(),
                    "Too many failed attempts, account temporarily locked. "
                            + "Try again in " + LOCK_MINUTES + " minutes.");
        }
    }

    @Override
    public int recordFailure(String account) {
        RAtomicLong counter = redissonClient.getAtomicLong(KEY_PREFIX + account);
        long value = counter.incrementAndGet();
        if (value == 1L) {
            counter.expire(Duration.ofMinutes(LOCK_MINUTES));
        }
        return (int) value;
    }

    @Override
    public void recordSuccess(String account) {
        redissonClient.getAtomicLong(KEY_PREFIX + account).delete();
    }
}
