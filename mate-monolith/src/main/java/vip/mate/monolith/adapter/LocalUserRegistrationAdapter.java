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
import vip.mate.api.system.command.RegisterUserCommand;
import vip.mate.api.system.response.UserInfoResponse;
import vip.mate.auth.domain.adapter.port.UserRegistrationPort;
import vip.mate.auth.domain.model.aggregate.AuthUser;
import vip.mate.base.exception.BizException;
import vip.mate.base.response.ResponseCode;
import vip.mate.system.application.command.UserCommandService;
import vip.mate.system.application.query.IUserQueryService;

/**
 * In-process implementation of {@link UserRegistrationPort} for monolith mode.
 * Directly calls mate-system's command/query services instead of Dubbo RPC.
 *
 * @author mateaix
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mate.rpc.mode", havingValue = "local")
public class LocalUserRegistrationAdapter implements UserRegistrationPort {

    private final UserCommandService userCommandService;
    private final IUserQueryService userQueryService;

    @Override
    public AuthUser register(RegisterUserCommand command) {
        try {
            String userId = userCommandService.createUser(command);
            UserInfoResponse info = userQueryService.findById(userId);
            if (info == null) {
                throw new BizException(ResponseCode.FAILURE.getCode(),
                        "Registration succeeded but user not found: " + userId);
            }
            return AuthUser.from(info);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("[monolith] registerUser failed", e);
            throw new BizException(ResponseCode.INTERNAL_ERROR.getCode(),
                    "Failed to create user: " + e.getMessage());
        }
    }
}
