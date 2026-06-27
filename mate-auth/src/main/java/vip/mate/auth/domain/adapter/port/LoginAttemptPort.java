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
package vip.mate.auth.domain.adapter.port;

/**
 * Outbound port: track failed login attempts so we can rate-limit / lock
 * an account under brute-force attack.
 *
 * @author mateaix
 */
public interface LoginAttemptPort {

    /**
     * Throw {@link vip.mate.base.exception.BizException} if the account has
     * exceeded the lockout threshold.
     */
    void ensureNotLocked(String account);

    /**
     * Record a failed attempt. Returns the current consecutive-failure count.
     */
    int recordFailure(String account);

    /**
     * Reset the failure counter on successful login.
     */
    void recordSuccess(String account);
}
