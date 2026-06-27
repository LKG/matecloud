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
package vip.mate.notice.integration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import vip.mate.notice.application.command.NoticeCommandService;
import vip.mate.notice.types.enums.BusinessType;
import vip.mate.notice.types.enums.NoticeChannel;
import vip.mate.starter.test.annotation.MateIntegrationTest;
import vip.mate.starter.test.container.MysqlContainerInitializer;
import vip.mate.starter.test.container.RedisContainerInitializer;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * End-to-end happy-path integration test for the notice send pipeline.
 *
 * <p>Boots the full Spring context (port 9050 randomized), starts MySQL and
 * Redis Testcontainers, runs Flyway migrations, then exercises:
 *
 * <pre>
 * NoticeCommandService.send(SMS, ..., templateParams)
 *   → NoticeRepositoryImpl.save (PENDING)
 *   → SmsNoticeAdapter.send
 *     → LogSmsProvider.send  (default; no real gateway)
 *   → NoticeRepositoryImpl.update (SUCCESS)
 * </pre>
 *
 * <p>Requires Docker. Disabled in CI by default; enable via {@code -Dintegration-test=true}.
 */
@Disabled("Requires Docker for Testcontainers — enable with -Dintegration-test=true")
@MateIntegrationTest
@ContextConfiguration(initializers = {
        MysqlContainerInitializer.class,
        RedisContainerInitializer.class,
})
class NoticeFlowIntegrationTest {

    @Autowired
    private NoticeCommandService noticeCommandService;

    @Test
    void send_smsWithTemplateParams_persistsAndDispatches() {
        String noticeId = noticeCommandService.send(
                NoticeChannel.SMS,
                "13800138000",
                BusinessType.VERIFY_CODE,
                "Your code is 123456",
                Map.of("code", "123456"));

        assertNotNull(noticeId);
        // The repository write went through Flyway-migrated mate_notice;
        // status transitions are covered by the unit test in
        // application/command/NoticeCommandServiceTest.
    }
}
