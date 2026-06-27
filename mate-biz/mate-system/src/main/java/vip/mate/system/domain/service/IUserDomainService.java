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
package vip.mate.system.domain.service;

import vip.mate.system.domain.model.aggregate.UserAggregate;

public interface IUserDomainService {

    UserAggregate createUser(String username, String password, String mobile, String email, String realName);

    UserAggregate findByIdOrThrow(String userId);

    UserAggregate changeRealName(String userId, String realName);

    /**
     * Apply a partial profile update. {@code null} fields are left untouched.
     */
    UserAggregate updateProfile(String userId, String realName, String mobile,
                                String email, String avatar, Integer gender);

    /**
     * Self-service password change. Verifies {@code rawOldPassword} matches the
     * stored hash, then re-encodes {@code rawNewPassword} with BCrypt.
     */
    UserAggregate changePassword(String userId, String rawOldPassword, String rawNewPassword);

    /**
     * Admin-driven reset — no old-password check. Authority is verified
     * upstream by Sa-Token's permission filter.
     */
    UserAggregate resetPassword(String userId, String rawNewPassword);

    UserAggregate freezeUser(String userId);

    UserAggregate unfreezeUser(String userId);

    UserAggregate deleteUser(String userId);
}
