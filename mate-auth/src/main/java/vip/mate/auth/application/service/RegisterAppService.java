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
package vip.mate.auth.application.service;

import org.springframework.stereotype.Service;
import vip.mate.auth.application.command.RegisterCommand;
import vip.mate.auth.domain.model.valobj.LoginResult;
import vip.mate.base.exception.BizException;
import vip.mate.base.response.ResponseCode;

/**
 * Public self-registration is DISABLED.
 * <p>
 * The back-office account subject is {@code mate_admin}; admin accounts are
 * created by an administrator via 管理员管理 (Admin management), not self-served.
 * This service is kept (the controller/route still reference it) but fails
 * closed on every call.
 *
 * @author mateaix
 */
@Service
public class RegisterAppService {

    public LoginResult register(RegisterCommand cmd) {
        throw new BizException(ResponseCode.FORBIDDEN.getCode(),
                "Public registration is disabled. Contact an administrator to create an account.");
    }
}
