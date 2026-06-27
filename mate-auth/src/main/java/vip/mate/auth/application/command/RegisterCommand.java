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
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * User self-registration command. Captcha is always required. If
 * {@code mobile} is present, an SMS code must also have been verified in
 * advance.
 *
 * @author mateaix
 */
@Data
public class RegisterCommand {

    @NotBlank
    @Size(min = 4, max = 32)
    @Pattern(regexp = "^[a-zA-Z0-9_]+$",
            message = "Username can only contain letters, digits, and underscores")
    private String username;

    @NotBlank
    @Size(min = 6, max = 64)
    private String password;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Invalid mobile number")
    private String mobile;

    /** Optional — SMS code received by {@code mobile}. */
    @Size(max = 6, message = "SMS code must be at most 6 characters")
    private String smsCode;

    @Size(max = 255, message = "Email is too long")
    private String email;

    @Size(max = 64, message = "Real name is too long")
    private String realName;

    /** captcha-plus one-time token from slider verification (required). */
    @NotBlank
    @Size(max = 1024, message = "captchaVerification is too long")
    private String captchaVerification;
}
