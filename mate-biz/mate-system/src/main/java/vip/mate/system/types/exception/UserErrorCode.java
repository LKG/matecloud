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
package vip.mate.system.types.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import vip.mate.base.exception.ErrorCode;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements ErrorCode {

    REQUEST_PARAM_NULL("U001", "Request parameter is null"),
    DUPLICATE_MOBILE("U002", "Mobile number already exists"),
    DUPLICATE_USERNAME("U007", "Username already exists"),
    USER_NOT_EXIST("U003", "User does not exist"),
    USER_IS_FROZEN("U004", "User is frozen"),
    USER_IS_DELETED("U005", "User is deleted"),
    USER_STATUS_INVALID("U006", "Invalid user status"),
    OLD_PASSWORD_INCORRECT("U008", "Old password incorrect"),
    PASSWORD_TOO_SHORT("U009", "Password must be at least 6 characters"),
    NOT_LOGGED_IN("U010", "User not logged in");

    private final String code;
    private final String message;
}
