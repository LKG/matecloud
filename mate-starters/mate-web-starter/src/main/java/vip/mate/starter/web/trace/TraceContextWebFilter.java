package vip.mate.starter.web.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import vip.mate.base.constant.AuthHeaders;
import vip.mate.base.trace.MateTrace;

import java.io.IOException;

/**
 * HTTP 入口的链路收尾过滤器。注册顺序在 Micrometer 观测过滤器<b>之内</b>
 * (order = HIGHEST_PRECEDENCE + 10), 故当服务装了 {@code mate-monitor-starter} 时,
 * 进到这里 MDC 的 traceId 已由 OTel 写好 —— 本过滤器<b>不抢</b>, 只做两件 OTel 不管的事:
 *
 * <ol>
 *   <li><b>回写 {@code X-Trace-Id} 响应头</b>:把当前 traceId 透给前端/调用方,
 *       报障时一报一个准(damai_pro 的运营点);</li>
 *   <li><b>兜底</b>:当服务<b>没装</b> tracing(无 active span, MDC 为空)时,
 *       接受上游 {@code X-Trace-Id} 续传, 否则新生成一个, 保证这条 HTTP 入口的日志也有 id。</li>
 * </ol>
 *
 * @author mateaix
 */
public class TraceContextWebFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        boolean fellBack = false;
        if (!MateTrace.hasCurrent()) {
            String inbound = request.getHeader(AuthHeaders.TRACE_ID);
            MateTrace.bind((inbound != null && !inbound.isEmpty()) ? inbound : MateTrace.newTraceId());
            fellBack = true;
        }
        try {
            String traceId = MateTrace.current();
            if (traceId != null && !response.isCommitted()) {
                response.setHeader(AuthHeaders.TRACE_ID, traceId);
            }
            filterChain.doFilter(request, response);
        } finally {
            if (fellBack) {
                MateTrace.clear();
            }
        }
    }
}
