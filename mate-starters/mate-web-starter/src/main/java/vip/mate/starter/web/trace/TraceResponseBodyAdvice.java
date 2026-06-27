package vip.mate.starter.web.trace;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import vip.mate.base.result.Result;
import vip.mate.base.trace.MateTrace;

/**
 * 给所有 {@link Result} 类型的 HTTP 响应体自动盖上当前 traceId —— 一处覆盖<b>控制器正常返回</b>
 * 与 {@code GlobalExceptionHandler} 的<b>异常返回</b>(两者都经 body advice 写出), 业务代码零侵入。
 *
 * <p>运行在请求线程, 此时 OTel 已把 traceId 写进 MDC, 直接取用。只在 traceId 缺失时盖,
 * 不覆盖上游/RPC 已带的值。前端从响应体读 {@code traceId}(在 body 里, 无跨域读响应头的限制)。
 *
 * @author mateaix
 */
@ControllerAdvice
public class TraceResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof Result<?> result && result.getTraceId() == null) {
            String traceId = MateTrace.current();
            if (traceId != null && !traceId.isEmpty()) {
                result.traceId(traceId);
            }
        }
        return body;
    }
}
