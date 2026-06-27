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
package vip.mate.starter.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import vip.mate.base.exception.ErrorCode;

@Getter
@AllArgsConstructor
public enum SecurityErrorCode implements ErrorCode {

    SIGN_EXPIRED("S0001", "API signature expired"),
    REPLAY_ATTACK("S0002", "Replay attack detected (duplicate nonce)"),
    SIGN_INVALID("S0003", "API signature verification failed"),
    APP_KEY_NOT_FOUND("S0004", "App key not found or disabled"),
    REPEAT_SUBMIT("S0010", "Repeat submission, please wait"),
    RATE_LIMITED("S0020", "Request rate exceeded, please slow down");

    private final String code;
    private final String message;
}
