package vip.mate.starter.rpc.trace;

import io.micrometer.tracing.TraceContext;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import vip.mate.base.trace.MateTrace;

/**
 * Dubbo consumer 端链路透传 —— {@link TraceDubboProviderFilter} 的对侧。
 *
 * <p>两条都带上, 让 provider 端按能力择优:</p>
 * <ol>
 *   <li><b>真实 OTel 上下文</b>:Micrometer 就绪时, 用 {@link io.micrometer.tracing.propagation.Propagator}
 *       把当前 span 的 W3C 上下文注入 RpcContext attachment, provider 据此<b>接续同一条 trace</b>;</li>
 *   <li><b>日志侧 traceId 字符串</b>:始终把 MDC 里的 traceId 作为 attachment 兜底,
 *       即便对端没有 OTel, 日志仍能用同一个 id 串起来。</li>
 * </ol>
 *
 * <p>order 比租户过滤器(-1000)更靠外(-1100), 保证 trace 包住一切。
 *
 * @author mateaix
 */
@Activate(group = "consumer", order = -1100)
public class TraceDubboConsumerFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (MateTracing.isActive()) {
            TraceContext ctx = MateTracing.tracer().currentTraceContext().context();
            if (ctx != null) {
                MateTracing.propagator().inject(ctx, invocation,
                        (carrier, key, value) -> carrier.setAttachment(key, value));
            }
        }
        String traceId = MateTrace.current();
        if (traceId != null && !traceId.isEmpty()) {
            invocation.setAttachment(MateTrace.MDC_KEY, traceId);
        }
        return invoker.invoke(invocation);
    }
}
