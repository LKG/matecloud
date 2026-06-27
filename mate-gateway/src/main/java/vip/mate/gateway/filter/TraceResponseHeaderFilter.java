package vip.mate.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import vip.mate.base.constant.AuthHeaders;

/**
 * 网关侧给<b>每个响应</b>盖上 {@code X-Trace-Id} —— 补齐「网关自产响应」(限流 429、鉴权失败、
 * 404 等没有下游可回写的场景)缺少关联 id 的缺口。代理成功的请求由下游服务回写, 这里只在缺失时补。
 *
 * <p><b>id 取值与对齐</b>:本 filter 在请求进入时调用 {@link GatewayTrace#capture(ServerWebExchange)},
 * 从 WebFlux 已就绪的 active observation 抓出真实 OTel traceId 存进 exchange;{@code beforeCommit}
 * 与各日志点({@code RequestLogFilter}/{@code HeaderRelayFilter}/sa-token)都经 {@link GatewayTrace}
 * 复用<b>同一个</b> id, 故客户端拿到的 {@code X-Trace-Id} 与网关日志里的 traceId <b>一致可关联</b>。
 * OTel 经 W3C 传播把同一 traceId 带到下游, 全链路单一 id, 无双 id。抓不到(无 active span)时
 * {@link GatewayTrace#capture} 兜底现生成一个, 保证响应与日志一定有 id。
 *
 * @author mateaix
 */
@Slf4j
@Component
public class TraceResponseHeaderFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        exchange.getResponse().beforeCommit(() -> {
            HttpHeaders headers = exchange.getResponse().getHeaders();
            if (headers.getFirst(AuthHeaders.TRACE_ID) == null) {
                // resolve: 已抓到的真实 id 优先, 否则此刻兜底生成并存回, 与日志点复用同一个。
                headers.set(AuthHeaders.TRACE_ID, GatewayTrace.resolve(exchange));
            }
            return Mono.empty();
        });
        // 「能抓就抓」真实 traceId 存进 exchange(此刻无 span 不强行 mint, 留给后续日志点/响应头
        // 在更晚时点 resolve), 供网关所有日志点与响应头复用同一个 id —— 与线程无关。
        GatewayTrace.capture(exchange);
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // 尽早注册 beforeCommit 钩子; 钩子在响应提交前统一执行, 与其它过滤器顺序无强耦合。
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
