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
package vip.mate.auth.infrastructure.adapter.user;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import vip.mate.api.rpc.RpcConstants;
import vip.mate.api.system.command.RegisterUserCommand;
import vip.mate.api.system.response.UserInfoResponse;
import vip.mate.api.system.service.IRpcUserService;
import vip.mate.auth.domain.adapter.port.UserRegistrationPort;
import vip.mate.auth.domain.model.aggregate.AuthUser;
import vip.mate.base.exception.BizException;
import vip.mate.base.response.ResponseCode;
import vip.mate.base.result.Result;

/**
 * Delegates user creation to mate-system via Dubbo RPC.
 *
 * @author mateaix
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "mate.rpc.mode", havingValue = "dubbo", matchIfMissing = true)
public class DubboUserRegistrationAdapter implements UserRegistrationPort {

    @DubboReference(id = "rpcUserServiceForRegistration", check = false, timeout = 5000,
            retries = RpcConstants.NO_RETRY,
            version = RpcConstants.VERSION, group = RpcConstants.GROUP_SYSTEM)
    private IRpcUserService rpcUserService;

    @Override
    public AuthUser register(RegisterUserCommand command) {
        try {
            Result<UserInfoResponse> result = rpcUserService.registerUser(command);
            if (result == null || !Boolean.TRUE.equals(result.getSuccess()) || result.getData() == null) {
                String msg = result == null ? "rpc returned null" : result.getMsg();
                throw new BizException(ResponseCode.FAILURE.getCode(), "Registration failed: " + msg);
            }
            return AuthUser.from(result.getData());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("[auth] registerUser RPC failed", e);
            throw new BizException(ResponseCode.INTERNAL_ERROR.getCode(),
                    "Failed to create user: " + e.getMessage());
        }
    }
}
