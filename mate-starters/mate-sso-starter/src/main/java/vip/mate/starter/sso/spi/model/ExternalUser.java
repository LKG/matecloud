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
package vip.mate.starter.sso.spi.model;

import java.util.List;
import java.util.Map;

/**
 * Vendor-neutral user, produced by an {@code IdentityProvider} after translating
 * the provider's raw payload (Anti-Corruption Layer). The {@code mate-sso-starter}
 * core and the consuming domain only ever see this shape.
 *
 * @param externalId  the provider-side stable user id (e.g. WeChat Work userid,
 *                    LDAP entryUUID) — the join key for identity mapping
 * @param unionId     cross-app stable id where the vendor provides one (DingTalk /
 *                    WeChat unionid); used to unify the same person across apps
 * @param name        display name
 * @param mobile      mobile number (may be null/desensitized)
 * @param email       email (LDAP often uses uid as email)
 * @param avatar      avatar url (may be null)
 * @param deptExternalIds external department ids this user belongs to
 * @param enabled     {@code false} = disabled/left (e.g. AD userAccountControl)
 * @param raw         original payload for providers that need extra fields
 *
 * @author mateaix
 */
public record ExternalUser(
        String externalId,
        String unionId,
        String name,
        String mobile,
        String email,
        String avatar,
        List<String> deptExternalIds,
        boolean enabled,
        Map<String, Object> raw) {
}
