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
package vip.mate.system.admin.types.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import vip.mate.base.exception.ErrorCode;

@Getter
@AllArgsConstructor
public enum AdminErrorCode implements ErrorCode {

    ADMIN_NOT_EXIST("A001", "Admin does not exist"),
    DUPLICATE_ADMIN_USERNAME("A002", "Admin username already exists"),
    ROLE_NOT_EXIST("A003", "Role does not exist"),
    MENU_NOT_EXIST("A004", "Menu does not exist"),
    DICT_TYPE_NOT_EXIST("A005", "Dict type does not exist"),
    DICT_DATA_NOT_EXIST("A006", "Dict data does not exist"),
    CONFIG_NOT_EXIST("A007", "Config does not exist"),
    DUPLICATE_CONFIG_KEY("SYSB010", "Config key already exists"),
    CANNOT_DELETE_BUILTIN_CONFIG("SYSB011", "Cannot delete built-in config"),
    DUPLICATE_DICT_TYPE_CODE("A012", "Dict type code already exists"),
    PASSWORD_TOO_SHORT("A013", "Password must be at least 6 characters"),
    PASSWORD_INCORRECT("A014", "Current password is incorrect"),
    DUPLICATE_ROLE_KEY("A015", "Role key already exists");

    private final String code;
    private final String message;
}
