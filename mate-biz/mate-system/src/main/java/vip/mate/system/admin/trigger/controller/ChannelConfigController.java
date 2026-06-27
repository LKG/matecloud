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
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vip.mate.base.result.Result;
import vip.mate.system.admin.application.channel.ChannelConfigService;
import vip.mate.system.admin.application.channel.ChannelConfigService.ChannelView;
import vip.mate.system.admin.application.channel.ChannelConfigService.TestResult;
import vip.mate.system.admin.trigger.annotation.OperationLog;

import java.util.Map;

/**
 * Admin REST surface for channel (storage / sms) provider configuration.
 * Backs the "渠道配置" page: list providers + their fields/values, save a
 * provider's config (and set it active), and run a connectivity / test-send.
 *
 * @author mateaix
 */
@RestController
@RequestMapping("/api/v1/admin/channel")
@RequiredArgsConstructor
@SaCheckLogin
public class ChannelConfigController {

    private final ChannelConfigService service;

    /** List all providers of a channel with their field metadata + (masked) values. */
    @GetMapping("/{channel}/providers")
    public Result<ChannelView> list(@PathVariable String channel) {
        return Result.ok(service.list(channel));
    }

    /** Save a provider's config; {@code enabled=true} also makes it the active provider. */
    @PutMapping("/{channel}/providers/{type}")
    @OperationLog(module = "渠道配置", type = "修改", description = "保存渠道配置")
    public Result<Void> save(@PathVariable String channel,
                             @PathVariable String type,
                             @RequestBody SaveReq req) {
        service.save(channel, type, req.enabled(), req.values());
        return Result.ok();
    }

    /** Test connectivity (storage: probe upload) / test-send (sms: to {@code target}). */
    @PostMapping("/{channel}/providers/{type}/test")
    public Result<TestResult> test(@PathVariable String channel,
                                   @PathVariable String type,
                                   @RequestBody(required = false) TestReq req) {
        String target = req == null ? null : req.target();
        return Result.ok(service.test(channel, type, target));
    }

    public record SaveReq(boolean enabled, Map<String, Object> values) {}

    public record TestReq(String target) {}
}
