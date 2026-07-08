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
package vip.mate.auth.domain.service.impl;

import lombok.RequiredArgsConstructor;
import vip.mate.auth.domain.adapter.port.PasswordEncoderPort;
import vip.mate.auth.domain.adapter.port.UserQueryPort;
import vip.mate.auth.domain.model.aggregate.AuthUser;
import vip.mate.auth.domain.model.valobj.Account;
import vip.mate.auth.domain.service.IAuthDomainService;
import vip.mate.base.exception.BizException;
import vip.mate.base.response.ResponseCode;

/**
 * Framework-free domain service (no Spring annotations) — registered as a bean by
 * {@code DomainServiceConfiguration} in the infrastructure layer, so the domain
 * layer keeps zero framework dependencies. Orchestrates the port lookup and
 * delegates every verification step to {@link AuthUser} domain methods.
 *
 * @author mateaix
 */
@RequiredArgsConstructor
public class AuthDomainServiceImpl implements IAuthDomainService {

    private final UserQueryPort userQueryPort;
    private final PasswordEncoderPort passwordEncoder;

    @Override
    public AuthUser loadAndVerify(Account account, String rawPassword) {
        AuthUser user = userQueryPort.findByAccount(account);
        // Use the generic "invalid account or password" message in both cases
        // so attackers cannot distinguish "no such account" from "wrong password".
        if (user == null) {
            throw new BizException(ResponseCode.UNAUTHORIZED.getCode(),
                    "Invalid account or password");
        }
        user.verifyPassword(rawPassword, passwordEncoder);
        user.ensureActive();
        return user;
    }

    @Override
    public AuthUser loadByMobileForSmsLogin(String mobile) {
        AuthUser user = userQueryPort.findByAccount(Account.mobile(mobile));
        if (user == null) {
            throw new BizException(ResponseCode.NOT_FOUND.getCode(),
                    "No account registered with this mobile number");
        }
        user.ensureActive();
        return user;
    }

    @Override
    public AuthUser loadById(String userId) {
        AuthUser user = userQueryPort.findById(userId);
        if (user == null) {
            throw new BizException(ResponseCode.NOT_FOUND.getCode(), "User not found");
        }
        return user;
    }
}
