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
package vip.mate.notice.trigger.rpc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import vip.mate.base.response.ResponseCode;
import vip.mate.base.result.Result;
import vip.mate.notice.application.command.NoticeCommandService;
import vip.mate.notice.types.enums.BusinessType;
import vip.mate.notice.types.enums.NoticeChannel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RpcNoticeServiceImplTest {

    private NoticeCommandService commandService;
    private RpcNoticeServiceImpl rpc;

    @BeforeEach
    void setUp() {
        commandService = mock(NoticeCommandService.class);
        rpc = new RpcNoticeServiceImpl(commandService);
    }

    @Test
    void sendSms_validBusinessType_returnsNoticeId() {
        when(commandService.send(eq(NoticeChannel.SMS), eq("13800138000"),
                eq(BusinessType.VERIFY_CODE), eq("hello")))
                .thenReturn("notice-1");

        Result<String> result = rpc.sendSms("13800138000", "hello", "VERIFY_CODE");

        assertTrue(result.getSuccess());
        assertEquals("notice-1", result.getData());
        verify(commandService).send(NoticeChannel.SMS, "13800138000",
                BusinessType.VERIFY_CODE, "hello");
    }

    @Test
    void sendSms_unknownBusinessType_returnsParamError() {
        Result<String> result = rpc.sendSms("13800138000", "hi", "NOT_A_TYPE");

        assertFalse(result.getSuccess());
        assertEquals(ResponseCode.PARAM_VALID_ERROR.getCode(), result.getCode());
    }

    @Test
    void sendSms_blankBusinessType_defaultsToVerifyCode() {
        when(commandService.send(eq(NoticeChannel.SMS), eq("13800138000"),
                eq(BusinessType.VERIFY_CODE), eq("hi")))
                .thenReturn("notice-2");

        Result<String> result = rpc.sendSms("13800138000", "hi", "");

        assertTrue(result.getSuccess());
    }

    @Test
    void sendSms_serviceThrows_returnsInternalError() {
        when(commandService.send(eq(NoticeChannel.SMS), eq("13800138000"),
                eq(BusinessType.VERIFY_CODE), eq("hi")))
                .thenThrow(new RuntimeException("gateway down"));

        Result<String> result = rpc.sendSms("13800138000", "hi", "VERIFY_CODE");

        assertFalse(result.getSuccess());
        assertEquals(ResponseCode.INTERNAL_ERROR.getCode(), result.getCode());
    }

    @Test
    void sendEmail_concatsSubjectIntoBody() {
        when(commandService.send(eq(NoticeChannel.EMAIL), eq("a@b.com"),
                eq(BusinessType.SYSTEM_ALERT), eq("[Subject]\nbody")))
                .thenReturn("notice-3");

        Result<String> result = rpc.sendEmail("a@b.com", "Subject", "body", "SYSTEM_ALERT");

        assertTrue(result.getSuccess());
    }
}
