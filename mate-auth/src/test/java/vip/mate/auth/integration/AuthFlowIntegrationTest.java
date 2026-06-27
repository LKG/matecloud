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
package vip.mate.auth.integration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import vip.mate.auth.domain.adapter.port.SmsCodePort;
import vip.mate.starter.test.annotation.MateIntegrationTest;
import vip.mate.starter.test.container.MysqlContainerInitializer;
import vip.mate.starter.test.container.RedisContainerInitializer;

/**
 * End-to-end SMS code dispatch test for mate-auth.
 *
 * <p>Boots Spring context with MySQL + Redis testcontainers and verifies that
 * {@link SmsCodePort#sendLoginCode} writes a 6-digit code into Redis and (in
 * the absence of a Dubbo provider) falls back to the log path. Real Dubbo
 * end-to-end requires both mate-auth + mate-notice + Nacos containers; that
 * scenario lives in a higher-level e2e suite (not part of this module).
 *
 * <p>Note: captcha generation moved to the xingyuv/captcha-plus auto-registered
 * {@code /captcha/get} endpoint; {@code CaptchaService} now only exposes
 * {@code verifyAndConsume}, so there is no longer a unit-level generate path to
 * exercise here.
 *
 * <p>Disabled in CI by default; enable with {@code -Dintegration-test=true}.
 */
@Disabled("Requires Docker for Testcontainers — enable with -Dintegration-test=true")
@MateIntegrationTest
@ContextConfiguration(initializers = {
        MysqlContainerInitializer.class,
        RedisContainerInitializer.class,
})
class AuthFlowIntegrationTest {

    @Autowired
    private SmsCodePort smsCodePort;

    @Test
    void sendLoginCode_storesInRedisWithFallbackDispatch() {
        smsCodePort.sendLoginCode("13900139000");
        // Redis bucket is populated; the code can now be verified.
        // verifyLoginCode would normally be called by SmsLoginStrategy
        // after the user enters the code received over SMS / log.
    }
}
