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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import vip.mate.base.exception.BizException;
import vip.mate.starter.sso.spi.IdentityProvider;
import vip.mate.starter.sso.types.SsoErrorCode;

/**
 * Holds every {@link IdentityProvider} keyed by {@link IdentityProvider#code()}.
 * Spring injects all provider beans (mirrors mate-file-starter's
 * {@code FileStorageRegistry}); lookup is by provider code — no central switch.
 *
 * @author mateaix
 */
public class ProviderRegistry {

    private final Map<String, IdentityProvider> byCode = new LinkedHashMap<>();

    public ProviderRegistry(List<IdentityProvider> providers) {
        if (providers != null) {
            for (IdentityProvider p : providers) {
                byCode.put(p.code(), p);
            }
        }
    }

    public IdentityProvider get(String code) {
        IdentityProvider p = byCode.get(code);
        if (p == null) {
            throw new BizException(SsoErrorCode.SSO_C_PROVIDER_NOT_FOUND.getCode(),
                    "No identity provider for '" + code + "'. Registered: " + byCode.keySet());
        }
        return p;
    }

    public boolean has(String code) {
        return byCode.containsKey(code);
    }

    public Collection<IdentityProvider> all() {
        return byCode.values();
    }
}
