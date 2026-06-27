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
package vip.mate.starter.satoken;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Wires Sa-Token's annotation interceptor into Spring MVC.
 * <p>
 * Without this, controllers using {@code @SaCheckLogin}, {@code @SaCheckPermission},
 * {@code @SaCheckRole} are silently skipped — the annotations are inert
 * unless an interceptor or an AOP weave is present. This starter intentionally
 * picks the lighter-weight interceptor route over Spring AOP because almost
 * every protected endpoint sits behind controllers, and the overhead of an
 * AOP advice across the whole bean graph is wasted.
 *
 * <p>The interceptor verifies token presence via {@link StpUtil#checkLogin()}
 * for paths matching {@code addPathPatterns} below. Public endpoints
 * (login / register / captcha) are excluded so they remain reachable without
 * a token. Method-level {@code @SaCheck*} annotations layer additional
 * fine-grained checks (permission codes, role codes) on top.
 *
 * @author mateaix
 */
@Configuration
@ConditionalOnClass(SaInterceptor.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SaTokenWebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // SaInterceptor with no auth predicate runs only the @SaCheck* annotations.
        // We do NOT call StpUtil.checkLogin() unconditionally here — login is
        // declared per-endpoint via @SaCheckLogin so public endpoints stay open
        // by default. Each protected controller is responsible for declaring
        // its own check, which keeps the security posture explicit per route.
        InterceptorRegistration registration = registry.addInterceptor(new SaInterceptor());
        registration.addPathPatterns("/**");
        registration.excludePathPatterns(
                // Static + framework
                "/error",
                "/favicon.ico",
                "/actuator/**",
                "/doc.html",
                "/swagger-resources/**",
                "/v3/api-docs/**",
                "/webjars/**"
        );
    }
}
