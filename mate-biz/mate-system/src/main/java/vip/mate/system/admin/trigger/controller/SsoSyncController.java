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
package vip.mate.system.admin.trigger.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vip.mate.base.result.Result;
import vip.mate.starter.sso.core.SsoTemplate;
import vip.mate.starter.sso.core.sync.SyncMode;
import vip.mate.starter.sso.core.sync.SyncResult;

/**
 * Manual trigger for SSO organization sync (企业微信 / 钉钉 / 飞书 / LDAP →
 * mate_dept + mate_admin). Only registered when {@code mate.feature.sso.enabled=true}.
 *
 * <p>The scheduled variant is a {@code @MateJobHandler} (follow-up); webhook-driven
 * sync is handled by the starter's callback controller.
 *
 * @author mateaix
 */
@RestController
@RequestMapping("/api/v1/admin/sso")
@RequiredArgsConstructor
@SaCheckLogin
@ConditionalOnProperty(prefix = "mate.feature.sso", name = "enabled", havingValue = "true")
public class SsoSyncController {

    private final SsoTemplate ssoTemplate;

    /** Pull the org tree + members for a provider and land them locally. */
    @PostMapping("/sync/{provider}")
    @SaCheckPermission("system:sso:sync")
    public Result<SyncResult> sync(@PathVariable String provider,
                                   @RequestParam(defaultValue = "FULL") String mode) {
        SyncMode syncMode = "INCREMENTAL".equalsIgnoreCase(mode) ? SyncMode.INCREMENTAL : SyncMode.FULL;
        return Result.ok(ssoTemplate.sync(provider, syncMode));
    }
}
