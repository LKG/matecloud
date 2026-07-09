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
package vip.mate.monolith.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Monolith security configuration.
 * <p>
 * In microservice mode the gateway (WebFlux) handles token validation before
 * requests reach individual services. In monolith mode there is no gateway,
 * so this interceptor takes over that responsibility by requiring a valid
 * Sa-Token session on all {@code /api/**} endpoints except the public ones.
 * <p>
 * The public list mirrors the gateway's {@code mate.gateway.auth.public-paths}
 * — these are all pre-login endpoints that carry no token (login / register /
 * captcha / SMS / SSO / LDAP). Note the captcha path needs the {@code /**}
 * suffix: the real endpoints are {@code /api/v1/auth/captcha/get|check} (see
 * {@code CaptchaPlusConfig}); excluding only {@code /api/v1/auth/captcha} let the
 * interceptor 401 the image fetch, so the slider puzzle never loaded.
 * (Non-{@code /api} paths like {@code /doc.html} aren't matched by
 * {@code /api/**}, so they need no entry here.)
 *
 * @author mateaix
 */
@Configuration
@ConditionalOnProperty(name = "mate.rpc.mode", havingValue = "local")
public class MonolithSecurityConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                // 纳入 /actuator/** 拦截 —— SaInterceptor 只覆盖 addPathPatterns, 不加则 actuator
                // 敏感端点(prometheus/metrics)在单体端口(9010)无鉴权暴露。health/info 探针放行,
                // 其余需登录 (与网关侧收窄一致; prometheus 抓取需另配 basic-auth 或独立管理端口)。
                .addPathPatterns("/api/**", "/actuator/**")
                .excludePathPatterns(
                        "/api/v1/auth/login",
                        "/api/v1/auth/register",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/captcha/**",
                        "/api/v1/auth/sms/**",
                        "/api/v1/auth/sso/**",
                        "/api/v1/auth/ldap/login",
                        "/api/v1/sso/callback/**",
                        "/actuator/health",
                        "/actuator/info"
                );
    }
}
