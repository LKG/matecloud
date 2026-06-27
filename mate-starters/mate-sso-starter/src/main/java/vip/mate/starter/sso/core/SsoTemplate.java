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
package vip.mate.starter.sso.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import vip.mate.base.channel.FieldSpec;
import vip.mate.base.exception.BizException;
import vip.mate.starter.sso.core.sync.SyncMode;
import vip.mate.starter.sso.core.sync.SyncPipeline;
import vip.mate.starter.sso.core.sync.SyncResult;
import vip.mate.starter.sso.port.IdentityMappingPort;
import vip.mate.starter.sso.spi.IdentityProvider;
import vip.mate.starter.sso.spi.JsSdkProvider;
import vip.mate.starter.sso.spi.model.AuthRequest;
import vip.mate.starter.sso.spi.model.ExternalUser;
import vip.mate.starter.sso.spi.model.JsSdkConfig;
import vip.mate.starter.sso.spi.model.JsSdkType;
import vip.mate.starter.sso.spi.model.ProviderConfig;
import vip.mate.starter.sso.types.SsoErrorCode;

/**
 * Facade over the SSO capability — the single entry point for callers (login
 * strategies, the manual/scheduled/webhook sync triggers). Hides the registry,
 * config store and pipeline behind two verbs: {@code authenticate} and {@code sync}.
 *
 * @author mateaix
 */
public class SsoTemplate {

    private final ProviderRegistry registry;
    private final SsoConfigStore configStore;
    private final IdentityMappingPort mappingPort;
    private final SyncPipeline pipeline;

    public SsoTemplate(ProviderRegistry registry,
                       SsoConfigStore configStore,
                       IdentityMappingPort mappingPort,
                       SyncPipeline pipeline) {
        this.registry = registry;
        this.configStore = configStore;
        this.mappingPort = mappingPort;
        this.pipeline = pipeline;
    }

    /** Authenticate against a provider and return the vendor-neutral user. */
    public ExternalUser authenticate(String providerCode, AuthRequest request) {
        IdentityProvider provider = registry.get(providerCode);
        ProviderConfig cfg = configStore.config(providerCode);
        return provider.authenticate(request, cfg);
    }

    /**
     * Authenticate, then resolve the bound local user id (empty if not yet bound).
     * Login strategies use this to decide login vs. just-in-time provisioning.
     */
    public Optional<String> authenticateAndResolve(String providerCode, AuthRequest request) {
        ExternalUser user = authenticate(providerCode, request);
        return mappingPort.resolveUser(providerCode, user.externalId(), user.unionId());
    }

    /** Run an organization sync. */
    public SyncResult sync(String providerCode, SyncMode mode) {
        return pipeline.run(providerCode, mode);
    }

    /**
     * Build a JS-SDK config signature (WeChat Work H5 {@code wx.config} /
     * {@code wx.agentConfig}). Throws if the provider has no H5 capability.
     */
    public JsSdkConfig jsConfig(String providerCode, JsSdkType type, String url) {
        IdentityProvider provider = registry.get(providerCode);
        if (!(provider instanceof JsSdkProvider jsSdk)) {
            throw new BizException(SsoErrorCode.SSO_C_PROVIDER_DISABLED.getCode(),
                    "Provider '" + providerCode + "' does not support JS-SDK signature");
        }
        return jsSdk.jsConfig(type, url, configStore.config(providerCode));
    }

    public ProviderRegistry registry() {
        return registry;
    }

    /**
     * Public, pre-login view of the configured providers for the login page:
     * code / display name / auth kind + the provider's <b>non-secret</b> config
     * fields (e.g. corpId, agentId) so the front end can render the scan panel.
     * Secret fields (secret / appSecret / adminPass) are never included.
     */
    public List<Map<String, Object>> publicProviders() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (IdentityProvider p : registry.all()) {
            ProviderConfig cfg = configStore.config(p.code());
            if (cfg.isEmpty()) {
                continue; // not configured → not shown
            }
            Map<String, String> pub = new LinkedHashMap<>();
            for (FieldSpec f : p.descriptor().getFields()) {
                if (!f.secret()) {
                    String v = cfg.get(f.key());
                    if (v != null && !v.isBlank()) {
                        pub.put(f.key(), v);
                    }
                }
            }
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("code", p.code());
            view.put("name", p.descriptor().getName());
            view.put("authKind", p.authKind().name());
            view.put("config", pub);
            out.add(view);
        }
        return out;
    }
}

