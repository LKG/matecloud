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
package vip.mate.ai.types.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import vip.mate.base.exception.ErrorCode;

@Getter
@AllArgsConstructor
public enum AiErrorCode implements ErrorCode {

    CONVERSATION_NOT_EXIST("AIB001", "Conversation does not exist"),
    NOT_OWNER_OF_CONVERSATION("AIB002", "You are not the owner of this conversation"),
    EMPTY_USER_MESSAGE("AIA001", "User message must not be blank"),
    AGENT_NOT_EXIST("AIB003", "Agent does not exist"),
    DUPLICATE_AGENT_CODE("AIB004", "Agent code already exists"),
    CANNOT_DELETE_BUILTIN_AGENT("AIB005", "Cannot delete built-in agent"),
    MCP_NOT_EXIST("AIB006", "MCP server does not exist"),
    DUPLICATE_MCP_CODE("AIB007", "MCP server code already exists"),
    PROVIDER_NOT_EXIST("AIB008", "Provider does not exist"),
    DUPLICATE_PROVIDER_CODE("AIB009", "Provider code already exists"),
    PROVIDER_API_KEY_REQUIRED("AIA002", "Provider API key is required");

    private final String code;
    private final String message;
}
