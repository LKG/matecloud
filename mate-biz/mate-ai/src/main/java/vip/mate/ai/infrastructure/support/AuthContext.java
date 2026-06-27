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
package vip.mate.ai.infrastructure.support;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import vip.mate.base.constant.AuthHeaders;

/**
 * Read the relayed user context from the current servlet request.
 * <p>
 * The gateway's {@code HeaderRelayFilter} injects {@code X-User-Id},
 * {@code X-User-Name} and {@code X-Tenant-Id} after successful Sa-Token
 * authentication. Downstream services should NEVER trust these headers
 * if the request did not come through the gateway — the
 * {@link vip.mate.starter.ai.config.AiAuthInterceptor} guards on that.
 *
 * @author mateaix
 */
public final class AuthContext {

    public static final String HEADER_USER_ID = AuthHeaders.USER_ID;
    public static final String HEADER_USER_NAME = AuthHeaders.USER_NAME;
    public static final String HEADER_TENANT_ID = AuthHeaders.TENANT_ID;

    private AuthContext() {}

    public static HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }

    public static String currentUserId() {
        HttpServletRequest req = currentRequest();
        return req == null ? null : req.getHeader(HEADER_USER_ID);
    }

    public static String currentUserName() {
        HttpServletRequest req = currentRequest();
        return req == null ? null : req.getHeader(HEADER_USER_NAME);
    }

    public static String currentTenantId() {
        HttpServletRequest req = currentRequest();
        return req == null ? null : req.getHeader(HEADER_TENANT_ID);
    }
}
