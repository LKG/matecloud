package vip.mate.starter.rpc.trace;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import vip.mate.base.trace.MateTrace;

/**
 * Dubbo provider 端把上游透传的链路上下文落到本线程 —— 这正是「业务日志没 traceId」的
 * 修复点:库模式的 Micrometer/OTel <b>不会</b>自动埋点 Dubbo, provider 线程默认没有 active
 * span, MDC 为空, 于是被 Dubbo 调进来的业务代码打的日志 traceId 全空。
 *
 * <p>两种模式(与 {@link TraceDubboConsumerFilter} 对应):</p>
 * <ol>
 *   <li><b>OTel 就绪</b>:从 attachment 抽取上游上下文, 起一个 server span 并设为当前,
 *       Micrometer 关联机制随即把<b>同一条 trace</b> 的 traceId/spanId 写进 MDC,
 *       Tempo/Jaeger 里 span 树也接续上;</li>
 *   <li><b>OTel 不在</b>:退化为把 attachment 里的 traceId 字符串绑到 MDC(没有则新生成),
 *       保证日志侧仍有连续 id。</li>
 * </ol>
 *
 * <p>无论哪条都在 {@code finally} 收尾, 池化 provider 线程绝不串号。
 *
 * @author mateaix
 */
@Activate(group = "provider", order = -1100)
public class TraceDubboProviderFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (MateTracing.isActive()) {
            Tracer tracer = MateTracing.tracer();
            Span span = MateTracing.propagator()
                    .extract(invocation, Invocation::getAttachment)
                    .name("dubbo/" + invoker.getInterface().getSimpleName()
                            + "#" + invocation.getMethodName())
                    .start();
            try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
                return invoker.invoke(invocation);
            } finally {
                span.end();
            }
        }

        // Fallback: log-side traceId only (no Micrometer on the classpath).
        String inbound = invocation.getAttachment(MateTrace.MDC_KEY);
        String effective = (inbound != null && !inbound.isEmpty())
                ? inbound : MateTrace.newTraceId();
        try {
            MateTrace.bind(effective);
            return invoker.invoke(invocation);
        } finally {
            MateTrace.clear();
        }
    }
}
