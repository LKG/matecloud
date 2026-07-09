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
package vip.mate.notice.infrastructure.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import vip.mate.base.constant.AuthHeaders;
import vip.mate.base.security.GatewaySignature;

import java.io.IOException;

/**
 * Auth interceptor for notice endpoints.
 *
 * <p>Requires the gateway-injected {@link AuthHeaders#USER_ID} <b>and</b> — when
 * {@code signatureRequired} is on (default) — a valid gateway HMAC signature over
 * the whole identity-header set ({@link AuthHeaders#GATEWAY_SIGN} /
 * {@link AuthHeaders#GATEWAY_TS}). Without the signature check, anyone able to
 * reach this service port directly (bypassing the gateway) could send
 * {@code X-User-Id: 1} and impersonate the super-admin. With it, they'd need the
 * shared secret to forge a signature, which they don't have.
 *
 * <p>When {@code signatureRequired=false} (local direct-connect debugging) it
 * falls back to the legacy "X-User-Id present" check only.
 *
 * @author mateaix
 */
public class NoticeAuthInterceptor implements HandlerInterceptor {

    private final String secret;
    private final long skewMs;
    private final boolean signatureRequired;

    public NoticeAuthInterceptor(String secret, long skewMs, boolean signatureRequired) {
        this.secret = secret;
        this.skewMs = skewMs;
        this.signatureRequired = signatureRequired;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String userId = request.getHeader(AuthHeaders.USER_ID);
        if (userId == null || userId.isBlank()) {
            return unauthorized(response);
        }
        if (signatureRequired && !GatewaySignature.verify(secret, request::getHeader,
                request.getHeader(AuthHeaders.GATEWAY_TS), request.getHeader(AuthHeaders.GATEWAY_SIGN),
                skewMs, System.currentTimeMillis())) {
            return unauthorized(response);
        }
        return true;
    }

    private static boolean unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"401\",\"msg\":\"Authentication required\",\"data\":null}");
        return false;
    }
}
