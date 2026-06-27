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
package vip.mate.auth.infrastructure.adapter.sms;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import vip.mate.auth.domain.adapter.port.NoticeDispatcher;
import vip.mate.base.exception.BizException;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisSmsCodePortTest {

    private RedissonClient redissonClient;
    private NoticeDispatcher dispatcher;
    private RBucket<String> rateBucket;
    private RBucket<String> codeBucket;
    private RedisSmsCodePort port;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redissonClient = mock(RedissonClient.class);
        dispatcher = mock(NoticeDispatcher.class);
        rateBucket = mock(RBucket.class);
        codeBucket = mock(RBucket.class);
        when(redissonClient.<String>getBucket(startsWith("mate:auth:sms:rate:"))).thenReturn(rateBucket);
        when(redissonClient.<String>getBucket(startsWith("mate:auth:sms:code:"))).thenReturn(codeBucket);

        @SuppressWarnings("unchecked")
        ObjectProvider<NoticeDispatcher> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(dispatcher);
        port = new RedisSmsCodePort(redissonClient, provider);
    }

    @Test
    void sendLoginCode_firstTime_dispatchesAndStoresCode() {
        when(rateBucket.setIfAbsent(eq("1"), any(Duration.class))).thenReturn(true);
        when(dispatcher.dispatchSms(anyString(), anyString(), eq("VERIFY_CODE"))).thenReturn(true);

        port.sendLoginCode("13800138000");

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(codeBucket).set(codeCaptor.capture(), any(Duration.class));
        assertEquals(6, codeCaptor.getValue().length(), "code must be 6 digits");
        verify(dispatcher, times(1)).dispatchSms(eq("13800138000"), anyString(), eq("VERIFY_CODE"));
    }

    @Test
    void sendLoginCode_rateLimited_throws() {
        when(rateBucket.setIfAbsent(eq("1"), any(Duration.class))).thenReturn(false);

        assertThrows(BizException.class, () -> port.sendLoginCode("13800138000"));
    }

    @Test
    void verifyLoginCode_correctCode_passes() {
        when(codeBucket.getAndDelete()).thenReturn("123456");
        port.verifyLoginCode("13800138000", "123456");
    }

    @Test
    void verifyLoginCode_wrongCode_throws() {
        when(codeBucket.getAndDelete()).thenReturn("123456");
        assertThrows(BizException.class, () -> port.verifyLoginCode("13800138000", "654321"));
    }

    @Test
    void verifyLoginCode_expiredCode_throws() {
        when(codeBucket.getAndDelete()).thenReturn(null);
        assertThrows(BizException.class, () -> port.verifyLoginCode("13800138000", "123456"));
    }
}
