package vip.mate.gateway.filter;

import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import vip.mate.base.constant.AuthHeaders;

/**
 * 把网关当前 trace 上下文<b>注入转发给下游的请求头</b> —— 补齐「gateway 与下游各自一条 trace、
 * traceId 对不上」的缺口({@code AuthHeaders} 文档里提到却一直缺失的 {@code TraceGatewayFilter})。
 *
 * <h3>为什么必须注入 W3C {@code traceparent}</h3>
 * 下游(mate-system 等)装了 OTel, 每个请求自起 server observation。若收不到 {@code traceparent},
 * 它就<b>新开一条根 trace</b>, traceId 与网关无关。库模式的 Spring Cloud Gateway <b>不会</b>自动把
 * trace 上下文注入出站代理请求(不像 sleuth 时代有 HttpHeadersFilter), 故须显式注入 —— 这正是
 * {@link io.micrometer.tracing.propagation.Propagator} 的活, 等价于 {@code TraceDubboConsumerFilter}
 * 在 Dubbo 侧做的事, 只是换成 HTTP 头。注入后下游 OTel 抽取 {@code traceparent}, server span 成为
 * 网关 span 的子节点 —— <b>全链路单一 traceId</b>, 下游日志的 traceId 即网关的 traceId。
 *
 * <p>另带上 {@link AuthHeaders#TRACE_ID}({@code X-Trace-Id}):给无 OTel 的消费方 / 前端关联用,
 * 取值与 traceparent 同源(都是 {@link GatewayTrace#resolve}/active span 的 traceId), 三处一致。
 *
 * <p><b>顺序</b>:在 {@code SecurityHeaderFilter}(-200, 剥认证头)之后注入(避免被剥), 在路由发出之前。
 *
 * @author mateaix
 */
@Component
public class TracePropagationFilter implements GlobalFilter, Ordered {

    /** 可能为 null:服务未装 tracing 时无此 bean, 退化为只透传 X-Trace-Id。 */
    private final Propagator propagator;

    public TracePropagationFilter(ObjectProvider<Propagator> propagator) {
        this.propagator = propagator.getIfAvailable();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = GatewayTrace.resolve(exchange);
        TraceContext traceContext = GatewayTrace.currentTraceContext(exchange);
        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.set(AuthHeaders.TRACE_ID, traceId);
                    if (propagator != null && traceContext != null) {
                        propagator.inject(traceContext, headers,
                                (carrier, key, value) -> carrier.set(key, value));
                    }
                })
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override
    public int getOrder() {
        // SecurityHeaderFilter(-200)剥头之后、路由发出之前注入。
        return -150;
    }
}
