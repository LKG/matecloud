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
package vip.mate.auth.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vip.mate.auth.domain.model.valobj.LoginType;

/**
 * Username / mobile / email + password login.
 *
 * @author mateaix
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordLoginCommand implements LoginCommand {

    @NotBlank(message = "account is required")
    @Size(max = 255, message = "account is too long")
    private String account;

    @NotBlank(message = "password is required")
    @Size(max = 512, message = "password is too long")
    private String password;

    /**
     * One-time verification token produced by captcha-plus after a successful
     * slider interaction. Required on every password login.
     */
    @Size(max = 1024, message = "captchaVerification is too long")
    private String captchaVerification;

    @Override
    public LoginType loginType() {
        return LoginType.PASSWORD;
    }
}
