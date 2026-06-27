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
package vip.mate.auth.domain.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Login type discriminator. Used by {@code LoginContext} to select the right
 * {@code LoginStrategy} bean.
 *
 * @author mateaix
 */
@Getter
@AllArgsConstructor
public enum LoginType {

    /** Username / mobile / email + password. */
    PASSWORD("password", "账号密码登录"),

    /** Mobile + SMS verification code. */
    SMS("sms", "手机验证码登录"),

    /** Third-party SSO (企业微信 / 钉钉 / 飞书) via mate-sso-starter. */
    SSO("sso", "第三方扫码登录"),

    /** LDAP / AD directory bind. */
    LDAP("ldap", "LDAP 目录登录");

    private final String code;
    private final String description;

    public static LoginType fromCode(String code) {
        if (code == null) return PASSWORD;
        for (LoginType t : values()) {
            if (t.code.equalsIgnoreCase(code)) return t;
        }
        throw new IllegalArgumentException("Unknown login type: " + code);
    }
}
