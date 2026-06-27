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
package vip.mate.starter.web.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import vip.mate.starter.web.trace.TraceContextWebFilter;
import vip.mate.starter.web.trace.TraceResponseBodyAdvice;

/**
 * Auto-configuration for web layer.
 *
 * @author mateaix
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import({GlobalExceptionHandler.class, JacksonConfiguration.class, Jackson3Configuration.class, TraceResponseBodyAdvice.class})
public class WebAutoConfiguration {

    /**
     * 链路收尾过滤器。order 取 {@code HIGHEST_PRECEDENCE + 10} —— 在 Micrometer 观测过滤器
     * (+1) 与租户过滤器(+5/+6)之内运行, 这样 OTel 已建立的 traceId 已在 MDC 里, 本过滤器
     * 只负责回写响应头 + 无 tracing 时兜底, 不与 OTel 抢 MDC。
     */
    @Bean
    public FilterRegistrationBean<TraceContextWebFilter> traceContextWebFilter() {
        FilterRegistrationBean<TraceContextWebFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TraceContextWebFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
