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
package vip.mate.starter.sso.port;

/**
 * Context passed to the {@link OrgProvisionPort} so the consumer knows which
 * provider/tenant a sync belongs to (for tagging the {@code from_scene} / source).
 *
 * @param provider provider code, e.g. {@code "wechat_work"}
 * @param tenantId tenant scope ({@code null}/"GLOBAL" for single-tenant)
 *
 * @author mateaix
 */
public record ProviderRef(String provider, String tenantId) {

    public static ProviderRef of(String provider) {
        return new ProviderRef(provider, null);
    }
}
