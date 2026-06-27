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
package vip.mate.auth.trigger.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vip.mate.auth.application.command.RegisterCommand;
import vip.mate.auth.application.service.RegisterAppService;
import vip.mate.auth.domain.model.valobj.LoginResult;
import vip.mate.base.result.Result;

/**
 * User self-registration endpoint. After a successful register the user is
 * auto-logged-in — the response carries the session token, so the client can
 * use the same callback path as login.
 *
 * @author mateaix
 */
@RestController
@RequestMapping("/api/v1/auth/register")
@RequiredArgsConstructor
public class RegisterController {

    private final RegisterAppService registerAppService;

    @PostMapping
    public Result<LoginResult> register(@Valid @RequestBody RegisterCommand command) {
        return Result.ok(registerAppService.register(command));
    }
}
