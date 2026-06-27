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
package vip.mate.starter.rpc.trace;

import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import vip.mate.base.trace.MateTrace;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

/**
 * 验证 {@link TraceDubboConsumerFilter} 把当前线程的 traceId 作为 RpcContext attachment
 * 带给下游 —— 与 provider 端配合实现「跨 Dubbo 日志同 traceId」。无基础设施依赖。
 *
 * @author mateaix
 */
class TraceDubboConsumerFilterTest {

    private final TraceDubboConsumerFilter filter = new TraceDubboConsumerFilter();

    @AfterEach
    void cleanup() {
        MDC.clear();
    }

    @Test
    void putsCurrentTraceIdIntoAttachment() {
        MDC.put(MateTrace.MDC_KEY, "trace-123");

        Invocation inv = mock(Invocation.class);
        Invoker<?> invoker = mock(Invoker.class);
        when(invoker.invoke(inv)).thenReturn(mock(Result.class));

        filter.invoke(invoker, inv);

        verify(inv).setAttachment(MateTrace.MDC_KEY, "trace-123");
    }

    @Test
    void noAttachmentWhenNoCurrentTraceId() {
        // MDC 为空时不应塞空 traceId attachment。
        Invocation inv = mock(Invocation.class);
        Invoker<?> invoker = mock(Invoker.class);
        when(invoker.invoke(inv)).thenReturn(mock(Result.class));

        filter.invoke(invoker, inv);

        verify(inv, never()).setAttachment(eq(MateTrace.MDC_KEY), anyString());
    }
}
