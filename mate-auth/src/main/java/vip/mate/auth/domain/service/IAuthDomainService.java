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
package vip.mate.auth.domain.service;

import vip.mate.auth.domain.model.aggregate.AuthUser;
import vip.mate.auth.domain.model.valobj.Account;

/**
 * Domain service containing auth-specific business rules that can't live on a
 * single aggregate (because they need a repository / port lookup first).
 *
 * @author mateaix
 */
public interface IAuthDomainService {

    /**
     * Load a user and verify password. Throws {@link vip.mate.base.exception.BizException}
     * on failure with a deliberately generic message ("invalid account or password").
     */
    AuthUser loadAndVerify(Account account, String rawPassword);

    /**
     * Load a user by mobile for SMS login. Throws if not found or inactive.
     */
    AuthUser loadByMobileForSmsLogin(String mobile);

    /**
     * Load a user by id, throw if not found.
     */
    AuthUser loadById(String userId);
}
