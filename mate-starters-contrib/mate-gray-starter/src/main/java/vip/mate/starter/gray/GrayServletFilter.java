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
package vip.mate.starter.gray;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * 单体进程内灰度过滤器（路线 B）—— 在请求入口把"该用户应走哪个版本"解析进
 * {@link GrayContext}，供业务代码、{@code @GrayFeature} 切面、以及
 * {@link GrayDubboConsumerFilter} 消费。
 *
 * <p>决策优先级：</p>
 * <ol>
 *   <li>{@link GrayProperties#isTrustHeader()} 为 true 且请求带
 *       {@code X-Gray-Version} 头时，直接采用（信任反代/边缘注入）；</li>
 *   <li>否则按 {@link GrayProperties#getUserIdHeader()}（单体里由
 *       {@code MonolithHeaderRelayFilter} 注入的 {@code X-User-Id}）取用户，
 *       交给 {@link GrayDecider} 走 Redis 白名单 + 百分比稳定分桶；</li>
 *   <li>规则缺失 / 未登录 / 决策异常 → 版本为 {@code null} = 稳定版，
 *       与未接入灰度时行为完全一致。</li>
 * </ol>
 *
 * <p>必须在身份注入过滤器之后运行（能读到 {@code X-User-Id}）—— 由消费方
 * （mate-monolith）用 {@code FilterRegistrationBean} 显式排序，故本类不自我注册。</p>
 *
 * @author mateaix
 */
@Slf4j
public class GrayServletFilter extends OncePerRequestFilter {

    private final GrayDecider decider;
    private final GrayProperties props;

    public GrayServletFilter(GrayDecider decider, GrayProperties props) {
        this.decider = decider;
        this.props = props;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String userId = resolveUserId(request);
        String version = null;
        try {
            version = computeVersion(request, userId);
        } catch (Exception e) {
            // 决策失败绝不阻断业务 —— 降级稳定版。
            log.warn("[mate-gray] decide failed, serving stable: {}", e.getMessage());
        }
        GrayContext.setVersion(version);
        GrayContextHolder.set(props.getDefaultFeature(), userId);
        try {
            chain.doFilter(version == null ? request : new GrayVersionWrapper(request, version), response);
        } finally {
            GrayContext.clear();
            GrayContextHolder.clear();
        }
    }

    private String computeVersion(HttpServletRequest request, String userId) {
        if (props.isTrustHeader()) {
            String header = request.getHeader(GrayContext.HEADER_NAME);
            if (header != null && !header.isBlank()) {
                return header.trim();
            }
        }
        return decider.decide(props.getDefaultFeature(), userId);
    }

    /**
     * 只从 {@link GrayProperties#getUserIdHeader()} 取身份 —— 该头由
     * {@code MonolithHeaderRelayFilter} 在校验有效 token 后注入（或剥离伪造），
     * 本过滤器不重复解析 token。
     */
    private String resolveUserId(HttpServletRequest request) {
        String userId = request.getHeader(props.getUserIdHeader());
        return userId == null || userId.isBlank() ? null : userId.trim();
    }

    /** 把解析出的灰度版本回写为请求头，供下游过滤器/拦截器/控制器统一读取。 */
    private static final class GrayVersionWrapper extends HttpServletRequestWrapper {

        private final String version;

        GrayVersionWrapper(HttpServletRequest request, String version) {
            super(request);
            this.version = version;
        }

        @Override
        public String getHeader(String name) {
            return GrayContext.HEADER_NAME.equalsIgnoreCase(name) ? version : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return GrayContext.HEADER_NAME.equalsIgnoreCase(name)
                    ? Collections.enumeration(List.of(version))
                    : super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = new java.util.ArrayList<>(Collections.list(super.getHeaderNames()));
            names.removeIf(n -> GrayContext.HEADER_NAME.equalsIgnoreCase(n));
            names.add(GrayContext.HEADER_NAME);
            return Collections.enumeration(names);
        }
    }
}
