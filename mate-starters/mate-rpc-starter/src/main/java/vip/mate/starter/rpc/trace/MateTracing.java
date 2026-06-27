package vip.mate.starter.rpc.trace;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;

/**
 * Dubbo SPI 过滤器不是 Spring Bean, 拿不到容器里的 {@link Tracer}/{@link Propagator},
 * 故用本静态持有者桥接 —— 与 {@code TenantRuntime} 同款套路。
 *
 * <p>由 {@code RpcTraceAutoConfiguration} 在启动时(且仅当 classpath 有 Micrometer Tracing 时)
 * 注入。未注入时 {@link #isActive()} 为 false, Dubbo trace 过滤器自动退化为
 * 「MDC 字符串透传」—— 日志侧 traceId 仍连续, 只是 Tempo/Jaeger 里不接续 span 树。
 *
 * @author mateaix
 */
public final class MateTracing {

    private static volatile Tracer tracer;
    private static volatile Propagator propagator;

    private MateTracing() {
    }

    public static void set(Tracer tracer, Propagator propagator) {
        MateTracing.tracer = tracer;
        MateTracing.propagator = propagator;
    }

    /** Micrometer Tracing 是否就绪 —— 就绪则 Dubbo 两端做真实 OTel 上下文透传。 */
    public static boolean isActive() {
        return tracer != null && propagator != null;
    }

    public static Tracer tracer() {
        return tracer;
    }

    public static Propagator propagator() {
        return propagator;
    }
}
