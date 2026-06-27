package vip.mate.gateway.filter;

import io.micrometer.observation.Observation;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.handler.TracingObservationHandler;
import org.slf4j.MDC;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;
import org.springframework.web.server.ServerWebExchange;
import vip.mate.base.trace.MateTrace;

/**
 * 网关侧链路 id 的<b>单一出处</b> —— 解决「响应式网关日志里 {@code [%X{traceId}]} 永远为空」。
 *
 * <h3>为什么不能只靠 OTel 自动写 MDC</h3>
 * 库模式(Micrometer + OTel bridge)只在 observation <b>scope 打开的那一刻</b>、在<b>那个线程</b>上
 * 把 traceId 写进 SLF4J MDC。而网关的 {@code GlobalFilter} 日志跑在
 * {@code reactor-http-epoll}/{@code loomBoundedElastic} 上(sa-token 阻塞调用还会 offload 到
 * boundedElastic),这些线程上没有同步打开的 scope —— {@code Hooks.enableAutomaticContextPropagation()}
 * 还原的是 OTel <i>context</i>, 并不保证在每个日志线程上重新触发那次写 MDC 的动作。于是 MDC 为空。
 *
 * <h3>做法(对齐 damai_pro 的 MdcSubscriber:主动写,不等框架还原)</h3>
 * <ol>
 *   <li>从 WebFlux 在请求最外层(HttpWebHandlerAdapter)就放进 exchange attribute 的
 *       {@link ServerRequestObservationContext} 里, 顺着 {@link TracingObservationHandler.TracingContext}
 *       拿到<b>真实的 OTel traceId</b>(与 OTel 经 W3C 透传给下游的<b>同一个</b> id);该 attribute 与
 *       线程无关, 在 filter 链跑起来前就已就绪, 任何回调里都读得到, 故无需依赖 Reactor 上下文还原;</li>
 *   <li>各日志点用 {@link #scope(ServerWebExchange)} 临时把这个 id 绑回当前线程的 MDC, 打完即还原。</li>
 * </ol>
 *
 * @author mateaix
 */
public final class GatewayTrace {

    /** 抓到的 traceId 在 exchange 上的存放槽 —— 全网关日志/响应头共用同一个 id。 */
    public static final String ATTR = GatewayTrace.class.getName() + ".traceId";

    private GatewayTrace() {
    }

    /** 关闭即还原 MDC 的句柄, 配合 try-with-resources 使用。 */
    public interface MdcScope extends AutoCloseable {
        @Override
        void close();
    }

    /**
     * 从 WebFlux 放进 exchange 的 active observation 里抽取当前请求的真实 traceId
     * ({@link ServerRequestObservationContext} → {@link TracingObservationHandler.TracingContext} → span)。
     * 取不到(无 span / 任何内部异常)返回 {@code null} —— <b>绝不抛</b>, 它在每个请求的关键路径上,
     * 失败必须降级为「兜底生成 id」, 而不是把请求打成 500。
     */
    public static String extract(ServerWebExchange exchange) {
        // 注意: Observation.Context#get(key) 在键缺失时返回 null;
        // 切勿用 getOrDefault(key, supplier) —— 其第二参是 Supplier, 传 null 在缺失时会 NPE。
        try {
            Object value = exchange.getAttribute(ServerRequestObservationContext.CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE);
            if (value instanceof Observation.Context observationContext) {
                TracingObservationHandler.TracingContext tc =
                        observationContext.get(TracingObservationHandler.TracingContext.class);
                if (tc != null && tc.getSpan() != null) {
                    String traceId = tc.getSpan().context().traceId();
                    if (traceId != null && !traceId.isEmpty()) {
                        return traceId;
                    }
                }
            }
        } catch (Exception ignored) {
            // best-effort: 任何 tracing 内部变化都不该影响请求处理
        }
        return null;
    }

    /**
     * 只在拿到<b>真实</b> traceId 时存进 exchange ——<b>不兜底生成</b>。早期入口(sa-token WebFilter)
     * 调它「能抓就抓」;此刻若 span 尚未绑定, 留给后续 {@link #resolve} 在更晚、更可能有真实 id 的
     * 时点处理, 避免过早 mint 一个与下游对不上的兜底 id。幂等。
     */
    public static void capture(ServerWebExchange exchange) {
        if (get(exchange) != null) {
            return;
        }
        String real = extract(exchange);
        if (real != null) {
            exchange.getAttributes().putIfAbsent(ATTR, real);
        }
    }

    /**
     * 取本请求<b>确定的</b> traceId:已存 → 真实 span → 兜底生成。保证非空, 且 mint 后<b>存回</b>
     * exchange(用 {@code putIfAbsent} 防并发 mint 出两个 id), 所有日志点/响应头复用同一个。
     */
    public static String resolve(ServerWebExchange exchange) {
        String existing = get(exchange);
        if (existing != null) {
            return existing;
        }
        String id = extract(exchange);
        if (id == null) {
            id = MateTrace.newTraceId();
        }
        Object prior = exchange.getAttributes().putIfAbsent(ATTR, id);
        return (prior instanceof String s && !s.isEmpty()) ? s : id;
    }

    /**
     * 取本请求 active span 的完整 {@link TraceContext}(traceId + spanId + 采样标志)——
     * 给「出站注入 W3C traceparent 让下游接续同一条 trace」用。无 span / 异常返回 {@code null}。
     */
    public static TraceContext currentTraceContext(ServerWebExchange exchange) {
        try {
            Object value = exchange.getAttribute(ServerRequestObservationContext.CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE);
            if (value instanceof Observation.Context observationContext) {
                TracingObservationHandler.TracingContext tc =
                        observationContext.get(TracingObservationHandler.TracingContext.class);
                if (tc != null && tc.getSpan() != null) {
                    return tc.getSpan().context();
                }
            }
        } catch (Exception ignored) {
            // best-effort, 同 extract
        }
        return null;
    }

    /** 取本请求已抓到的 traceId(可能为 null)。 */
    public static String get(ServerWebExchange exchange) {
        Object captured = exchange.getAttribute(ATTR);
        return (captured instanceof String s && !s.isEmpty()) ? s : null;
    }

    /**
     * 把本请求的 traceId 临时绑到当前线程 MDC, 返回的句柄在 {@code close()} 时还原原值。
     * 经 {@link #resolve} 确保一定有 id(必要时兜底生成并存回), 故各日志点拿到的恒一致。
     * 用法:{@code try (var ignored = GatewayTrace.scope(exchange)) { log... }}
     */
    public static MdcScope scope(ServerWebExchange exchange) {
        String id = resolve(exchange);
        String previous = MDC.get(MateTrace.MDC_KEY);
        MDC.put(MateTrace.MDC_KEY, id);
        return () -> {
            if (previous != null) {
                MDC.put(MateTrace.MDC_KEY, previous);
            } else {
                MDC.remove(MateTrace.MDC_KEY);
            }
        };
    }
}
