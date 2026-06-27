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

import java.util.List;
import vip.mate.base.channel.ProviderDescriptor;
import vip.mate.starter.sso.spi.model.AuthRequest;
import vip.mate.starter.sso.spi.model.ExternalDept;
import vip.mate.starter.sso.spi.model.ExternalUser;
import vip.mate.starter.sso.spi.model.ProviderConfig;

/**
 * Identity-provider SPI — one implementation per channel (WeChat Work / DingTalk /
 * Feishu / LDAP). Annotate the impl with
 * {@link vip.mate.starter.sso.spi.annotation.IdentityProvider} and register it as a
 * bean; the {@code ProviderRegistry} keys it by {@link #code()}.
 *
 * <p>Implementations are an Anti-Corruption Layer: they translate vendor payloads
 * into the neutral {@link ExternalUser} / {@link ExternalDept}. The starter core
 * (login + sync) never touches a vendor API directly.
 *
 * @author mateaix
 */
public interface IdentityProvider {

    /** Provider code key, e.g. {@code "wechat_work"} (mirrors the annotation value). */
    String code();

    /** Self-description (display name + config fields) for the admin UI. */
    ProviderDescriptor descriptor();

    /** OAuth vs directory bind — selects the login path. */
    AuthKind authKind();

    /**
     * Authenticate one end user.
     * <ul>
     *   <li>OAUTH: consume {@link AuthRequest#code()} → exchange → user info.</li>
     *   <li>DIRECTORY_BIND: verify {@link AuthRequest#username()} /
     *       {@link AuthRequest#password()} against the directory.</li>
     * </ul>
     */
    ExternalUser authenticate(AuthRequest request, ProviderConfig config);

    /** Pull the full department tree (for organization sync). */
    List<ExternalDept> fetchDepartments(ProviderConfig config);

    /** Pull all members under the given departments (for organization sync). */
    List<ExternalUser> fetchUsers(ProviderConfig config, List<ExternalDept> departments);
}
