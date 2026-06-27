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
package vip.mate.system.tenant.types.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import vip.mate.base.exception.ErrorCode;

/**
 * Tenant quota / lifecycle error codes (RFC-028).
 *
 * <p>Code scheme per CLAUDE.md: {MODULE}{TYPE}{SEQ} — module {@code TEN},
 * type {@code B} = business rule violation.
 *
 * @author mateaix
 */
@Getter
@AllArgsConstructor
public enum TenantErrorCode implements ErrorCode {

    TENANT_NOT_FOUND("TENB001", "租户不存在"),
    PACKAGE_NOT_FOUND("TENB002", "套餐不存在"),
    USER_QUOTA_EXCEEDED("TENB003", "租户用户数已达套餐上限"),
    FEATURE_NOT_AVAILABLE("TENB004", "当前套餐不支持该功能"),
    TENANT_SUSPENDED("TENB005", "租户已被冻结"),
    TENANT_DELETED("TENB006", "租户已被删除"),
    TENANT_EXPIRED("TENB007", "租户已到期，请续费"),
    AI_NOT_AVAILABLE("TENB008", "当前套餐不支持 AI 功能"),
    AI_QUOTA_EXCEEDED("TENB009", "今日 AI 调用次数已达上限");

    private final String code;
    private final String message;
}
