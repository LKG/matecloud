package vip.mate.base.trace;

import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

/**
 * 线程上下文透传的<b>统一登记处</b> —— 解决「@Async / 线程池任务在 worker 线程上丢失
 * traceId(及租户等 ThreadLocal 上下文)」的问题, 等价于 damai_pro 的 {@code BaseThreadPool},
 * 但做成「各上下文各自登记、一个装饰器统一应用」, 避免多个 {@code TaskDecorator} 互相覆盖。
 *
 * <p><b>设计要点</b>:每个关注点(MDC/traceId、租户……)注册一个
 * {@link UnaryOperator}{@code <Runnable>} 包装器, 在<b>提交线程</b>捕获上下文、在
 * <b>worker 线程</b>还原并于结束后复位。{@link #decorate} 把它们串起来。各 starter 的
 * {@code BeanPostProcessor} 都把执行器的 {@code TaskDecorator} 设成
 * {@code MateThreadContext::decorate} —— 因为指向同一登记处, 谁覆盖谁都<b>等价</b>, 不再冲突。
 *
 * <p>纯 JDK + SLF4J, 零 Spring 依赖, 故落在 {@code mate-base}; MDC 透传<b>内建默认开启</b>。
 *
 * @author mateaix
 */
public final class MateThreadContext {

    private static final List<UnaryOperator<Runnable>> PROPAGATORS = new CopyOnWriteArrayList<>();

    static {
        // 内建: 跨线程透传整个 MDC(含 traceId/spanId), 无需任何额外配置。
        PROPAGATORS.add(mdcPropagator());
    }

    private MateThreadContext() {
    }

    /**
     * 注册一个上下文透传器(如租户)。幂等性由调用方保证(通常在启动时注册一次)。
     */
    public static void register(UnaryOperator<Runnable> propagator) {
        if (propagator != null) {
            PROPAGATORS.add(propagator);
        }
    }

    /**
     * 用全部已注册透传器包装任务 —— 直接可作 Spring {@code TaskDecorator}
     * (方法引用 {@code MateThreadContext::decorate})。
     */
    public static Runnable decorate(Runnable runnable) {
        Runnable wrapped = runnable;
        for (UnaryOperator<Runnable> propagator : PROPAGATORS) {
            wrapped = propagator.apply(wrapped);
        }
        return wrapped;
    }

    /**
     * 用全部已注册透传器包装 {@link Callable} —— 给 {@code CompletableFuture.supplyAsync}、
     * {@code ExecutorService.submit/invokeAll} 等<b>非 Spring 线程池</b>路径用(那些不走
     * {@code TaskDecorator}, 否则 worker 线程上 traceId 为空)。
     *
     * <p>上下文在<b>调用本方法的线程(=提交线程)</b>捕获(复用 {@link #decorate(Runnable)} 的
     * 透传器), worker 线程执行时还原、跑完复位。
     */
    public static <T> Callable<T> decorate(Callable<T> callable) {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Exception> error = new AtomicReference<>();
        // 在提交线程此刻捕获上下文(透传器在 decorate(Runnable) 内部 apply 时抓快照)。
        Runnable decorated = decorate(() -> {
            try {
                result.set(callable.call());
            } catch (Exception e) {
                error.set(e);
            }
        });
        return () -> {
            decorated.run();
            if (error.get() != null) {
                throw error.get();
            }
            return result.get();
        };
    }

    /**
     * 包装 {@link Executor}:经它提交的每个任务都自动透传上下文。适合
     * {@code CompletableFuture.runAsync(task, MateThreadContext.wrap(exec))} 或裸 Executor bean。
     */
    public static Executor wrap(Executor executor) {
        return command -> executor.execute(decorate(command));
    }

    /**
     * 包装 {@link ExecutorService}:{@code execute/submit/invokeAll/invokeAny} 提交的任务
     * 都自动透传上下文。适合 {@code Executors.newVirtualThreadPerTaskExecutor()} 等非 Spring 池。
     * 生命周期方法({@code shutdown/close/...})原样委派, {@code try-with-resources} 语义不变。
     */
    public static ExecutorService wrap(ExecutorService executor) {
        return new ContextPropagatingExecutorService(executor);
    }

    private static <T> Collection<? extends Callable<T>> decorateAll(Collection<? extends Callable<T>> tasks) {
        List<Callable<T>> out = new ArrayList<>(tasks.size());
        for (Callable<T> task : tasks) {
            out.add(decorate(task));
        }
        return out;
    }

    /**
     * 委派式 {@link ExecutorService}:提交路径包装任务以透传上下文, 其余原样转发。
     */
    private record ContextPropagatingExecutorService(ExecutorService delegate) implements ExecutorService {

        @Override
        public void execute(Runnable command) {
            delegate.execute(decorate(command));
        }

        @Override
        public Future<?> submit(Runnable task) {
            return delegate.submit(decorate(task));
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            return delegate.submit(decorate(task), result);
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            return delegate.submit(decorate(task));
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return delegate.invokeAll(decorateAll(tasks));
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                throws InterruptedException {
            return delegate.invokeAll(decorateAll(tasks), timeout, unit);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
                throws InterruptedException, ExecutionException {
            return delegate.invokeAny(decorateAll(tasks));
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return delegate.invokeAny(decorateAll(tasks), timeout, unit);
        }

        @Override
        public void shutdown() {
            delegate.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            return delegate.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return delegate.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return delegate.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return delegate.awaitTermination(timeout, unit);
        }

        @Override
        public void close() {
            delegate.close();
        }
    }

    /**
     * MDC 透传器:提交线程捕获 MDC 快照, worker 线程先存自身旧值、置入快照、跑完复位,
     * 池化线程绝不串号。
     */
    private static UnaryOperator<Runnable> mdcPropagator() {
        return runnable -> {
            Map<String, String> captured = MDC.getCopyOfContextMap();
            return () -> {
                Map<String, String> previous = MDC.getCopyOfContextMap();
                try {
                    if (captured != null) {
                        MDC.setContextMap(captured);
                    } else {
                        MDC.clear();
                    }
                    runnable.run();
                } finally {
                    if (previous != null) {
                        MDC.setContextMap(previous);
                    } else {
                        MDC.clear();
                    }
                }
            };
        };
    }
}
