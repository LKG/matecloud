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
 * Outbound port: generate, store, and verify one-time SMS login codes.
 * <p>
 * Redis-backed implementation does the actual storage. A separate adapter
 * handles the real SMS delivery (stubbed as log.info for now; production
 * wires to mate-notice via Dubbo).
 *
 * @author mateaix
 */
public interface SmsCodePort {

    /**
     * Generate a code, store it in Redis (TTL = 5 min), and dispatch to the
     * user's mobile. Rate-limited to 1 call per minute per mobile.
     */
    void sendLoginCode(String mobile);

    /**
     * Verify and consume the code. Throws on expired / mismatch.
     */
    void verifyLoginCode(String mobile, String code);
}
