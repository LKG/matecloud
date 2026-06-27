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
package vip.mate.starter.devtools;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;
import vip.mate.base.trace.MateTrace;

import java.io.IOException;

/**
 * Logs request URI + status + elapsed time for every HTTP request.
 * Active only in dev profile.
 *
 * @author mateaix
 */
@Slf4j
public class ApiTimingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        // 本过滤器在 Micrometer 观测过滤器之内(order +10), 此刻 MDC 已有 traceId —— 先抓下来。
        // 计时日志打在 finally 里, 那时请求内嵌套 scope(Dubbo/DB 客户端观测)关闭可能已把 traceId
        // 从 MDC 移除(micrometer 关 scope 是 remove 而非还原父值), 故收尾时按需绑回再还原。
        String traceId = MateTrace.current();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long cost = System.currentTimeMillis() - start;
            boolean rebind = traceId != null && !traceId.isEmpty() && MateTrace.current() == null;
            if (rebind) {
                MDC.put(MateTrace.MDC_KEY, traceId);
            }
            try {
                log.info("[devtools] {} {} -> {} ({} ms)",
                        request.getMethod(), request.getRequestURI(), response.getStatus(), cost);
            } finally {
                if (rebind) {
                    MDC.remove(MateTrace.MDC_KEY);
                }
            }
        }
    }
}
