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
package vip.mate.starter.sso.spi;

import vip.mate.starter.sso.core.AccessTokenCache;
import vip.mate.starter.sso.core.http.HttpExecutor;
import vip.mate.starter.sso.spi.model.AuthRequest;
import vip.mate.starter.sso.spi.model.ExternalUser;
import vip.mate.starter.sso.spi.model.ProviderConfig;

/**
 * Template Method for OAuth providers (WeChat Work / DingTalk / Feishu). Fixes the
 * invariant flow — <em>get access token (cached) → exchange code for external id →
 * fetch user detail</em> — once, so each concrete provider only fills the three
 * vendor-specific steps. This collapses PlayEdu's three near-identical OAuth
 * listeners into a single skeleton.
 *
 * @author mateaix
 */
public abstract class AbstractOAuthIdentityProvider implements IdentityProvider {

    protected final HttpExecutor http;
    protected final AccessTokenCache tokenCache;

    protected AbstractOAuthIdentityProvider(HttpExecutor http, AccessTokenCache tokenCache) {
        this.http = http;
        this.tokenCache = tokenCache;
    }

    @Override
    public final AuthKind authKind() {
        return AuthKind.OAUTH;
    }

    /** Fixed OAuth login skeleton — subclasses do not override this. */
    @Override
    public final ExternalUser authenticate(AuthRequest request, ProviderConfig config) {
        String accessToken = accessToken(config);
        String externalId = exchangeCodeForUserId(request.code(), accessToken, config);
        return fetchUserDetail(externalId, accessToken, config);
    }

    /** Cached access token (Cache-Aside); cache key = provider code. */
    protected String accessToken(ProviderConfig config) {
        return tokenCache.get("sso:token:" + code(), () -> requestAccessToken(config));
    }

    // ---- vendor-specific steps (the only thing concrete providers implement) ----

    /** Request a fresh access token from the vendor; return value + ttl seconds. */
    protected abstract AccessTokenCache.Token requestAccessToken(ProviderConfig config);

    /** Exchange the front-end login {@code code} for the vendor user id. */
    protected abstract String exchangeCodeForUserId(String code, String accessToken, ProviderConfig config);

    /** Fetch user detail and translate it into the neutral {@link ExternalUser}. */
    protected abstract ExternalUser fetchUserDetail(String externalId, String accessToken, ProviderConfig config);
}
