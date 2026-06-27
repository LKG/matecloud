package vip.mate.starter.rpc.config;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import vip.mate.starter.rpc.trace.MateTracing;

/**
 * 把容器里的 {@link Tracer}/{@link Propagator} 桥接给 Dubbo SPI trace 过滤器
 * (它们非 Spring Bean, 见 {@link MateTracing})。
 *
 * <p>仅当 classpath 有 Micrometer Tracing(即引入了 {@code mate-monitor-starter})时生效;
 * 否则 Dubbo trace 过滤器走纯 MDC 字符串透传, 不依赖本类。
 *
 * @author mateaix
 */
@AutoConfiguration
@ConditionalOnClass(name = "io.micrometer.tracing.Tracer")
public class RpcTraceAutoConfiguration {

    @Bean
    public InitializingBean mateTracingInitializer(ObjectProvider<Tracer> tracer,
                                                   ObjectProvider<Propagator> propagator) {
        return () -> {
            Tracer t = tracer.getIfAvailable();
            Propagator p = propagator.getIfAvailable();
            if (t != null && p != null) {
                MateTracing.set(t, p);
            }
        };
    }
}
