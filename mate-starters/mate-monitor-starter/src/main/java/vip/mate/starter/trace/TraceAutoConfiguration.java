/*
 * Copyright (c) 2024-2026 Beijing Daotiandi Technology Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package vip.mate.starter.trace;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for distributed tracing.
 * The heavy lifting is done by Spring Boot's auto-config for Micrometer Tracing + OTel.
 * This class exists as an entry point and logs activation.
 *
 * @author mateaix
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(name = "io.micrometer.tracing.Tracer")
public class TraceAutoConfiguration {

    public TraceAutoConfiguration() {
        log.info("[mate-trace] Micrometer Tracing auto-configuration activated");
    }

    /**
     * 打开 Reactor 自动上下文传播 —— 这是「响应式(网关 WebFlux) / 跨线程切换时 traceId 不丢」
     * 的关键, 也是 Boot4/Reactor 的官方做法, 取代 damai_pro 手写的 {@code MdcSubscriber + LogHooks}。
     *
     * <p>开启后, Micrometer Tracing 注册的 {@code ThreadLocalAccessor} 会让 OTel 的
     * trace 上下文随 Reactor {@code Context} 在算子链/线程间自动还原到 ThreadLocal,
     * 于是 MDC 的 {@code traceId} 在响应式日志里也有值, 虚拟线程同理。
     *
     * <p>仅当 classpath 有 Reactor(网关等响应式应用)时装配; 纯 Servlet 应用不需要。
     * 这是 JVM 级幂等开关, 重复调用无副作用。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = {"reactor.core.publisher.Hooks", "io.micrometer.context.ContextRegistry"})
    static class ReactorContextPropagationConfiguration {

        @PostConstruct
        public void enableAutomaticContextPropagation() {
            reactor.core.publisher.Hooks.enableAutomaticContextPropagation();
            log.info("[mate-trace] Reactor automatic context propagation enabled (traceId flows across reactive/threads)");
        }
    }
}
