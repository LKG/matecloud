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
package vip.mate.auth.application.command;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vip.mate.auth.domain.model.valobj.LoginType;

/**
 * Third-party SSO login (企业微信 / 钉钉 / 飞书). The front end obtains an OAuth
 * {@code code} (扫码 / H5),posts it here with the provider code; mate-auth
 * delegates to mate-sso-starter to resolve the external identity, then issues a
 * Sa-Token session for the bound local user.
 *
 * @author mateaix
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SsoLoginCommand implements LoginCommand {

    /** Provider code: wechat_work / dingtalk / feishu. */
    @NotBlank
    private String providerCode;

    /** OAuth authorization code from the front-end login widget. */
    @NotBlank
    private String code;

    private String state;

    private String redirectUri;

    @Override
    public LoginType loginType() {
        return LoginType.SSO;
    }
}
