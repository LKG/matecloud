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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import vip.mate.base.trace.MateThreadContext;
import vip.mate.base.trace.MateTrace;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 验证 {@link MateThreadContext} 真的把 MDC(traceId)跨线程透传 —— 这是 @Async/线程池
 * 任务日志带 traceId 的底层机制。直接执行真实代码, 无需任何基础设施。
 *
 * @author mateaix
 */
class MateThreadContextTest {

    @AfterEach
    void cleanup() {
        MDC.clear();
    }

    @Test
    void rebindsCapturedMdcOnWorkerThread() throws InterruptedException {
        MDC.put(MateTrace.MDC_KEY, "trace-abc");
        AtomicReference<String> seen = new AtomicReference<>("<unset>");

        // 在“提交线程”捕获, 然后在一条本身无 MDC 的 worker 线程上执行。
        Runnable decorated = MateThreadContext.decorate(
                () -> seen.set(MDC.get(MateTrace.MDC_KEY)));
        Thread worker = new Thread(decorated);
        worker.start();
        worker.join();

        assertEquals("trace-abc", seen.get(), "提交线程的 traceId 必须在 worker 线程上重现");
    }

    @Test
    void restoresWorkerPreviousMdcAfterRun() {
        MDC.put(MateTrace.MDC_KEY, "submit-id");
        Runnable decorated = MateThreadContext.decorate(() -> { /* no-op */ });

        // 模拟池化 worker 自带的旧 traceId。
        MDC.put(MateTrace.MDC_KEY, "worker-prev");
        decorated.run();

        assertEquals("worker-prev", MDC.get(MateTrace.MDC_KEY),
                "任务结束后必须还原 worker 线程原有的 traceId(池化线程不串号)");
    }

    @Test
    void clearsWorkerWhenItHadNoPreviousMdc() {
        MDC.put(MateTrace.MDC_KEY, "submit-id");
        Runnable decorated = MateThreadContext.decorate(() -> { /* no-op */ });

        // worker 起始无 MDC, 跑完必须仍然干净。
        MDC.clear();
        decorated.run();

        assertNull(MDC.get(MateTrace.MDC_KEY),
                "worker 线程原本无 traceId, 任务后必须清空");
    }

    @Test
    void decoratedCallableCarriesTraceIdAndReturnsValue() throws Exception {
        MDC.put(MateTrace.MDC_KEY, "trace-callable");
        AtomicReference<String> seen = new AtomicReference<>("<unset>");

        var callable = MateThreadContext.decorate(() -> {
            seen.set(MDC.get(MateTrace.MDC_KEY));
            return "result";
        });
        // 在无 MDC 的 worker 线程上跑。
        ExecutorService raw = Executors.newSingleThreadExecutor();
        try {
            assertEquals("result", raw.submit(callable).get(), "Callable 返回值必须原样透出");
        } finally {
            raw.shutdownNow();
        }
        assertEquals("trace-callable", seen.get(), "Callable 必须在 worker 线程上带上提交线程的 traceId");
    }

    @Test
    void wrappedExecutorServicePropagatesToSubmitAndInvokeAll() throws Exception {
        MDC.put(MateTrace.MDC_KEY, "trace-pool");

        // 裸虚拟线程池(不走 Spring TaskDecorator)经 wrap 后应自动透传。
        try (ExecutorService exec = MateThreadContext.wrap(Executors.newVirtualThreadPerTaskExecutor())) {
            Future<String> viaSubmit = exec.submit(() -> MDC.get(MateTrace.MDC_KEY));
            assertEquals("trace-pool", viaSubmit.get(), "submit(Callable) 必须透传 traceId");

            List<Future<String>> viaInvokeAll = exec.invokeAll(List.of(
                    () -> MDC.get(MateTrace.MDC_KEY),
                    () -> MDC.get(MateTrace.MDC_KEY)));
            for (Future<String> f : viaInvokeAll) {
                assertEquals("trace-pool", f.get(), "invokeAll 的每个任务都必须透传 traceId");
            }
        }
    }

    @Test
    void wrappedExecutorPropagatesToCompletableFuture() throws Exception {
        MDC.put(MateTrace.MDC_KEY, "trace-cf");

        ExecutorService raw = Executors.newVirtualThreadPerTaskExecutor();
        try {
            Executor wrapped = MateThreadContext.wrap((Executor) raw);
            String seen = CompletableFuture.supplyAsync(
                    () -> MDC.get(MateTrace.MDC_KEY), wrapped).get();
            assertEquals("trace-cf", seen, "经 wrap(Executor) 的 CompletableFuture 必须透传 traceId");
        } finally {
            raw.shutdownNow();
        }
    }
}
