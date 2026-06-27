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
package vip.mate.starter.sso.types;

import vip.mate.base.exception.ErrorCode;

/**
 * SSO error codes. Convention {MODULE}{TYPE}{SEQ}: module {@code SSO},
 * type A=param / B=business / C=rpc-or-config / D=db / E=external.
 *
 * @author mateaix
 */
public enum SsoErrorCode implements ErrorCode {

    SSO_A_BAD_REQUEST("SSOA001", "SSO 请求参数错误"),
    SSO_B_NOT_BOUND("SSOB001", "当前外部账号未绑定本地账号"),
    SSO_B_SYNC_RUNNING("SSOB002", "该渠道正在同步中,请稍后再试"),
    SSO_C_PROVIDER_NOT_FOUND("SSOC001", "未找到对应的身份源 provider"),
    SSO_C_PROVIDER_DISABLED("SSOC002", "该身份源未启用或未配置"),
    SSO_E_HTTP("SSOE001", "调用外部身份源接口失败"),
    SSO_E_LDAP("SSOE002", "LDAP/AD 通讯失败");

    private final String code;
    private final String message;

    SsoErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
