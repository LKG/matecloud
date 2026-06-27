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
 * LDAP / AD direct-bind login. Username may be a uid or an email; the password is
 * verified against the directory by mate-sso-starter's LdapProvider, then a
 * Sa-Token session is issued for the bound local user.
 *
 * @author mateaix
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LdapLoginCommand implements LoginCommand {

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    /** Behavior captcha token (one-time) — LDAP is password-based, so required. */
    private String captchaVerification;

    @Override
    public LoginType loginType() {
        return LoginType.LDAP;
    }
}
