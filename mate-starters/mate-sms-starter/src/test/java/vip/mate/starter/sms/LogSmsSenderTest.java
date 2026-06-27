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
package vip.mate.starter.sms;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import vip.mate.starter.sms.core.SmsSenderRegistry;
import vip.mate.starter.sms.model.SmsMessage;
import vip.mate.starter.sms.model.SmsResult;
import vip.mate.starter.sms.provider.LogSmsSender;

class LogSmsSenderTest {

    @Test
    void logSender_alwaysSucceeds() {
        LogSmsSender sender = new LogSmsSender();
        SmsResult result = sender.send(
                vip.mate.starter.sms.model.SmsMessage.of("13800138000", "code 123456", "VERIFY_CODE"));
        assertTrue(result.success());
        assertEquals("log", result.providerType());
    }

    @Test
    void registry_resolvesByTypeAndThrowsForUnknown() {
        SmsSenderRegistry registry = new SmsSenderRegistry(List.of(new LogSmsSender()));
        assertEquals("log", registry.get("log").type());
        assertThrows(RuntimeException.class, () -> registry.get("nope"));
    }
}
