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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vip.mate.auth.application.command.LdapLoginCommand;
import vip.mate.auth.application.command.PasswordLoginCommand;
import vip.mate.auth.application.command.RefreshTokenCommand;
import vip.mate.auth.application.command.SmsLoginCommand;
import vip.mate.auth.application.command.SmsSendCommand;
import vip.mate.auth.application.command.SsoLoginCommand;
import vip.mate.auth.application.captcha.CaptchaService;
import vip.mate.auth.application.service.AuthAppService;
import vip.mate.auth.domain.model.aggregate.AuthUser;
import vip.mate.auth.domain.model.valobj.LoginResult;
import vip.mate.base.result.Result;

import java.util.List;
import java.util.Map;

/**
 * Authentication endpoints. The gateway routes {@code /api/v1/auth/**} to
 * mate-auth (port 9020); all other services consume the Sa-Token session
 * issued here via the gateway's tenant + token filters.
 *
 * <p>Every login / SMS-send request must carry {@code captchaVerification} — a
 * one-time token issued by captcha-plus after a successful slider interaction
 * ({@code POST /captcha/check}).  The gateway rewrites
 * {@code /api/v1/auth/captcha/**} → {@code /captcha/**} on mate-auth.
 *
 * @author MateCloud
 *
 * @author mateaix
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthAppService authAppService;
    private final CaptchaService captchaService;

    /**
     * Username/password login.
     *
     * @param command username + password + optional captcha pair
     * @return token value + token name + role/permission set + tenant ID
     */
    @PostMapping("/login")
    public Result<LoginResult> passwordLogin(@Valid @RequestBody PasswordLoginCommand command) {
        captchaService.verifyAndConsume(command.getCaptchaVerification());
        return Result.ok(authAppService.passwordLogin(command));
    }

    /**
     * Dispatch a 6-digit verification code via mate-notice (Aliyun / Tencent /
     * log provider depending on configuration). Rate-limited to one send per
     * mobile per minute.
     *
     * @param command target mobile + optional captcha
     */
    @PostMapping("/sms/send")
    public Result<Void> sendSmsCode(@Valid @RequestBody SmsSendCommand command) {
        captchaService.verifyAndConsume(command.getCaptchaVerification());
        authAppService.sendSmsCode(command.getMobile());
        return Result.ok();
    }

    /**
     * SMS code login. The 6-digit code is one-shot — successful or failed
     * verification deletes the Redis entry so replay attempts always fail.
     *
     * @param command mobile + verification code
     * @return token value + token name + role/permission set + tenant ID
     */
    @PostMapping("/sms/login")
    public Result<LoginResult> smsLogin(@Valid @RequestBody SmsLoginCommand command) {
        return Result.ok(authAppService.smsLogin(command));
    }

    /**
     * Public list of configured SSO providers (non-secret config: corpId /
     * agentId …) so the login page can render the scan panel. Empty when SSO is
     * off. No auth required (pre-login).
     */
    @GetMapping("/sso/providers")
    public Result<List<Map<String, Object>>> ssoProviders() {
        return Result.ok(authAppService.ssoProviders());
    }

    /**
     * Third-party SSO login (企业微信 / 钉钉 / 飞书). Front end posts the OAuth
     * {@code code} + provider; requires {@code mate.feature.sso.enabled=true}.
     */
    @PostMapping("/sso/login")
    public Result<LoginResult> ssoLogin(@Valid @RequestBody SsoLoginCommand command) {
        return Result.ok(authAppService.ssoLogin(command));
    }

    /**
     * LDAP / AD login. Password-based, so a behavior-captcha token is required.
     */
    @PostMapping("/ldap/login")
    public Result<LoginResult> ldapLogin(@Valid @RequestBody LdapLoginCommand command) {
        captchaService.verifyAndConsume(command.getCaptchaVerification());
        return Result.ok(authAppService.ldapLogin(command));
    }

    /**
     * Silently renew the session: exchange a valid refresh token for a fresh
     * access + refresh token pair. Public (no active access token required) — the
     * client's request interceptor calls this on a 401 and replays the original
     * request with the new access token, so an active user is never bounced to the
     * login screen until the refresh token itself expires.
     *
     * @param command the opaque refresh token issued at login
     * @return a new {@link LoginResult} (new access token + rotated refresh token)
     */
    @PostMapping("/refresh")
    public Result<LoginResult> refresh(@Valid @RequestBody RefreshTokenCommand command) {
        return Result.ok(authAppService.refresh(command.getRefreshToken()));
    }

    /**
     * Revoke the current Sa-Token session and clear the cached role + permission
     * sets. Idempotent — safe to call when there is no active session.
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        authAppService.logout();
        return Result.ok();
    }

    /**
     * Resolve the {@link AuthUser} for the current Sa-Token session — used by
     * the admin UI to populate the user dropdown and permission directives.
     * Throws {@code 401} when no active session is present.
     */
    @GetMapping("/info")
    public Result<AuthUser> info() {
        return Result.ok(authAppService.currentUser());
    }
}
