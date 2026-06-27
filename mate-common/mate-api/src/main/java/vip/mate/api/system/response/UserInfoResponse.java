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
package vip.mate.api.system.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import vip.mate.api.system.enums.UserStatus;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * User information response DTO returned by RPC calls.
 *
 * @author mateaix
 */
@Data
@SuperBuilder
@NoArgsConstructor
public class UserInfoResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String userId;
    private String username;
    private String realName;
    private String mobile;
    private String email;
    private String avatar;
    /**
     * BCrypt-hashed password. Only populated for internal auth RPC;
     * MUST be cleared before returning to clients.
     */
    private String password;
    private UserStatus status;
    private List<String> roleCodes;
    private List<String> permissions;
    private String deptId;
    private String deptName;
    /**
     * Tenant ID the user belongs to. Null on legacy single-tenant rows; the
     * Sa-Token session falls back to the system tenant ("1") when missing.
     */
    private String tenantId;
    private LocalDateTime createdAt;
}
