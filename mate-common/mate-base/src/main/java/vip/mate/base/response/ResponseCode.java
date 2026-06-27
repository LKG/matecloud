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
package vip.mate.base.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import vip.mate.base.exception.ErrorCode;

/**
 * Platform-level response codes carried in the {@code Result.code} body field.
 *
 * <p><b>Code scheme</b> — a single, consistent namespace following the project
 * convention {@code {MODULE}{TYPE}{SEQ}} (see CLAUDE.md):
 * <ul>
 *   <li>MODULE — {@code SYS} (framework/system) or {@code SEC} (auth/security)</li>
 *   <li>TYPE — {@code A}=param, {@code B}=business, {@code C}=RPC,
 *       {@code D}=DB, {@code E}=external/upstream</li>
 *   <li>SEQ — 3-digit sequence</li>
 * </ul>
 * The sole exception is {@link #SUCCESS} = {@code "00000"} (universal success token).
 *
 * <p><b>Not HTTP status.</b> These codes intentionally do NOT mirror HTTP status
 * numbers. The transport-level HTTP status is set separately via
 * {@code @ResponseStatus} in the global exception handler, so {@code code} stays a
 * stable, machine-readable <em>business</em> identifier independent of transport.
 *
 * @author mateaix
 */
@Getter
@AllArgsConstructor
public enum ResponseCode implements ErrorCode {

    /** Universal success token. */
    SUCCESS("00000", "Operation successful"),

    // --- Param / request (TYPE A) ---
    BAD_REQUEST("SYSA001", "Bad request"),
    PARAM_VALID_ERROR("SYSA002", "Parameter validation failed"),
    METHOD_NOT_ALLOWED("SYSA003", "Method not allowed"),

    // --- Business (TYPE B) ---
    FAILURE("SYSB001", "Operation failed"),
    DATA_NOT_FOUND("SYSB002", "Data not found"),
    DUPLICATE_DATA("SYSB003", "Duplicate data"),
    OPERATION_NOT_ALLOWED("SYSB004", "Operation not allowed"),
    NOT_FOUND("SYSB005", "Resource not found"),
    CONFLICT("SYSB006", "Resource conflict"),

    // --- RPC (TYPE C) ---
    RPC_CALL_FAILED("SYSC001", "RPC call failed"),
    RPC_TIMEOUT("SYSC002", "RPC call timeout"),
    RPC_SERVICE_NOT_FOUND("SYSC003", "RPC service not found"),

    // --- External / upstream / system fault (TYPE E) ---
    INTERNAL_ERROR("SYSE001", "Internal server error"),
    SERVICE_UNAVAILABLE("SYSE002", "Service unavailable"),
    GATEWAY_TIMEOUT("SYSE003", "Gateway timeout"),

    // --- Auth / security (MODULE SEC) ---
    UNAUTHORIZED("SECB001", "Unauthorized"),
    FORBIDDEN("SECB002", "Forbidden"),
    TOO_MANY_REQUESTS("SECB003", "Too many requests");

    private final String code;
    private final String message;
}
