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
package vip.mate.monolith.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import vip.mate.api.system.response.UserInfoResponse;
import vip.mate.auth.domain.adapter.port.UserQueryPort;
import vip.mate.auth.domain.model.aggregate.AuthUser;
import vip.mate.auth.domain.model.valobj.Account;
import vip.mate.system.application.query.IUserQueryService;

/**
 * In-process implementation of {@link UserQueryPort} for monolith mode.
 * Directly calls mate-system's query service instead of Dubbo RPC.
 *
 * @author mateaix
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mate.rpc.mode", havingValue = "local")
public class LocalUserQueryAdapter implements UserQueryPort {

    private final IUserQueryService userQueryService;

    @Override
    public AuthUser findByAccount(Account account) {
        UserInfoResponse response = switch (account.kind()) {
            case USERNAME -> userQueryService.findByUsernameForAuth(account.value());
            case MOBILE   -> userQueryService.findByMobileForAuth(account.value());
            case EMAIL    -> userQueryService.findByUsernameForAuth(account.value());
        };
        if (response == null) {
            return null;
        }
        return AuthUser.from(response);
    }

    @Override
    public AuthUser findById(String userId) {
        UserInfoResponse response = userQueryService.findById(userId);
        if (response == null) {
            return null;
        }
        return AuthUser.from(response);
    }
}
