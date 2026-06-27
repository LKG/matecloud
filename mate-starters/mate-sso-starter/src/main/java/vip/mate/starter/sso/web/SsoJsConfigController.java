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
package vip.mate.starter.sso.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vip.mate.base.result.Result;
import vip.mate.starter.sso.core.SsoTemplate;
import vip.mate.starter.sso.spi.model.JsSdkConfig;
import vip.mate.starter.sso.spi.model.JsSdkType;

/**
 * H5 JS-SDK signature endpoint (generalizes PlayEdu's
 * {@code /api/v1/wechat/getConfigSignature} / {@code getAgentConfigSignature}).
 * Front end posts the current page url; gets back the signed config for
 * {@code wx.config} (type=corp) or {@code wx.agentConfig} (type=agent).
 *
 * <p>Registered only on a servlet web application (see {@code SsoAutoConfiguration}).
 *
 * @author mateaix
 */
@RestController
@RequestMapping("/api/v1/sso")
public class SsoJsConfigController {

    private final SsoTemplate ssoTemplate;

    public SsoJsConfigController(SsoTemplate ssoTemplate) {
        this.ssoTemplate = ssoTemplate;
    }

    @PostMapping("/jsconfig")
    public Result<JsSdkConfig> jsConfig(@RequestParam String provider,
                                        @RequestParam(defaultValue = "corp") String type,
                                        @RequestParam String url) {
        JsSdkType jsType = "agent".equalsIgnoreCase(type) ? JsSdkType.AGENT : JsSdkType.CORP;
        return Result.ok(ssoTemplate.jsConfig(provider, jsType, url));
    }
}
