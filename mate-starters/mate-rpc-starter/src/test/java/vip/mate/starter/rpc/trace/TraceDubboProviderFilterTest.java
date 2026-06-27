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
import org.apache.dubbo.rpc.RpcException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import vip.mate.base.trace.MateTrace;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证 {@link TraceDubboProviderFilter} 的兜底(无 Micrometer)分支 —— 这正是「业务日志没
 * traceId」的修复点:provider 线程把上游 attachment 里的 traceId 绑到 MDC, 业务代码打日志即带上,
 * 且池化线程跑完必清理(异常路径也清)。这条路径不依赖任何基础设施, 直接执行真实过滤器代码。
 *
 * <p>注:{@code MateTracing} 未注入(isActive=false), 故走兜底分支。
 *
 * @author mateaix
 */
class TraceDubboProviderFilterTest {

    private final TraceDubboProviderFilter filter = new TraceDubboProviderFilter();

    @AfterEach
    void cleanup() {
        MDC.clear();
    }

    @Test
    void bindsInboundTraceIdToMdcDuringInvokeAndClearsAfter() {
        Invocation inv = mock(Invocation.class);
        when(inv.getAttachment(MateTrace.MDC_KEY)).thenReturn("trace-xyz");

        AtomicReference<String> duringInvoke = new AtomicReference<>();
        Invoker<?> invoker = mock(Invoker.class);
        when(invoker.invoke(inv)).thenAnswer(i -> {
            duringInvoke.set(MDC.get(MateTrace.MDC_KEY));
            return mock(Result.class);
        });

        filter.invoke(invoker, inv);

        assertEquals("trace-xyz", duringInvoke.get(),
                "业务执行期间 MDC 必须带上上游透传的 traceId");
        assertNull(MDC.get(MateTrace.MDC_KEY), "provider 线程跑完必须清理 traceId");
    }

    @Test
    void generatesTraceIdWhenNoInboundAttachment() {
        Invocation inv = mock(Invocation.class);
        when(inv.getAttachment(MateTrace.MDC_KEY)).thenReturn(null);

        AtomicReference<String> duringInvoke = new AtomicReference<>();
        Invoker<?> invoker = mock(Invoker.class);
        when(invoker.invoke(inv)).thenAnswer(i -> {
            duringInvoke.set(MDC.get(MateTrace.MDC_KEY));
            return mock(Result.class);
        });

        filter.invoke(invoker, inv);

        assertNotNull(duringInvoke.get(), "无上游 traceId 时应自动生成一个, 保证日志可 grep");
        assertEquals(32, duringInvoke.get().length(), "兜底 traceId 取 32 位十六进制(与 OTel 等宽)");
        assertNull(MDC.get(MateTrace.MDC_KEY), "跑完必须清理");
    }

    @Test
    void clearsMdcEvenWhenDownstreamThrows() {
        Invocation inv = mock(Invocation.class);
        when(inv.getAttachment(MateTrace.MDC_KEY)).thenReturn("trace-err");
        Invoker<?> invoker = mock(Invoker.class);
        when(invoker.invoke(inv)).thenThrow(new RpcException("boom"));

        assertThrows(RpcException.class, () -> filter.invoke(invoker, inv));

        assertNull(MDC.get(MateTrace.MDC_KEY), "异常路径也必须在 finally 清理 traceId");
    }
}
