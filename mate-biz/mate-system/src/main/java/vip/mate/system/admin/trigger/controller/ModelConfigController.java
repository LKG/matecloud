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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vip.mate.base.result.Result;
import vip.mate.system.admin.application.model.ModelConfigService;
import vip.mate.system.admin.application.model.ModelConfigService.DescriptorView;
import vip.mate.system.admin.application.model.ModelConfigService.GatewayView;
import vip.mate.system.admin.application.model.ModelConfigService.ProviderView;
import vip.mate.system.admin.application.model.ModelConfigService.SystemModelView;
import vip.mate.system.admin.application.model.ModelConfigService.TestResult;
import vip.mate.system.admin.trigger.annotation.OperationLog;

import java.util.List;
import java.util.Map;

/**
 * Admin REST surface for the AI model configuration center. Backs the "模型配置" page:
 * manage providers (+ credentials), assign per-type defaults, and configure the
 * optional OpenAI-compatible gateway. All credentials are masked on read and never
 * returned in clear text.
 *
 * @author mateaix
 */
@RestController
@RequestMapping("/api/v1/admin/model")
@RequiredArgsConstructor
@SaCheckLogin
public class ModelConfigController {

    private final ModelConfigService service;

    // ---- providers ----

    /** List configured providers (secrets masked) for the current tenant. */
    @GetMapping("/providers")
    public Result<List<ProviderView>> providers() {
        return Result.ok(service.providers(service.currentTenant()));
    }

    /** Create or update a provider (id blank = create). Returns the provider id. */
    @PutMapping("/providers/{id}")
    @OperationLog(module = "模型配置", type = "修改", description = "保存模型供应商")
    public Result<String> saveProvider(@PathVariable String id, @RequestBody ProviderReq req) {
        String saved = service.saveProvider(blankToNull(id), req.vendor(), req.name(),
                req.modalities(), req.enabled(), req.sort(), req.values(), req.models(),
                service.currentTenant());
        return Result.ok(saved);
    }

    /** Create a new provider (id auto-assigned). Returns the provider id. */
    @PostMapping("/providers")
    @OperationLog(module = "模型配置", type = "新增", description = "新增模型供应商")
    public Result<String> createProvider(@RequestBody ProviderReq req) {
        String saved = service.saveProvider(null, req.vendor(), req.name(), req.modalities(),
                req.enabled(), req.sort(), req.values(), req.models(), service.currentTenant());
        return Result.ok(saved);
    }

    @DeleteMapping("/providers/{id}")
    @OperationLog(module = "模型配置", type = "删除", description = "删除模型供应商")
    public Result<Void> deleteProvider(@PathVariable String id) {
        service.deleteProvider(id);
        return Result.ok();
    }

    @PostMapping("/providers/{id}/test")
    public Result<TestResult> testProvider(@PathVariable String id) {
        return Result.ok(service.testProvider(id));
    }

    /** Live-fetch the provider's model list via its OpenAI-compatible /models endpoint. */
    @PostMapping("/providers/{id}/fetch-models")
    @OperationLog(module = "模型配置", type = "查询", description = "拉取供应商模型列表")
    public Result<List<String>> fetchModels(@PathVariable String id) {
        return Result.ok(service.fetchModels(id));
    }

    // ---- descriptors / modalities ----

    @GetMapping("/descriptors")
    public Result<List<DescriptorView>> descriptors() {
        return Result.ok(service.descriptors());
    }

    @GetMapping("/modalities")
    public Result<List<String>> modalities() {
        return Result.ok(service.modalities());
    }

    // ---- system models (defaults per type) ----

    @GetMapping("/system-models")
    public Result<List<SystemModelView>> systemModels() {
        return Result.ok(service.systemModels(service.currentTenant()));
    }

    @PutMapping("/system-models/{type}")
    @OperationLog(module = "模型配置", type = "修改", description = "设置系统默认模型")
    public Result<Void> saveSystemModel(@PathVariable String type, @RequestBody SystemModelReq req) {
        service.saveSystemModel(type, req.providerId(), req.model(), service.currentTenant());
        return Result.ok();
    }

    // ---- gateway ----

    @GetMapping("/gateway")
    public Result<GatewayView> gateway() {
        return Result.ok(service.gateway(service.currentTenant()));
    }

    @PutMapping("/gateway")
    @OperationLog(module = "模型配置", type = "修改", description = "保存模型网关")
    public Result<Void> saveGateway(@RequestBody GatewayReq req) {
        service.saveGateway(req.enabled(), req.gatewayType(), req.baseUrl(), req.token(),
                req.defaultGroup(), req.modelMapping(), service.currentTenant());
        return Result.ok();
    }

    @PostMapping("/gateway/test")
    public Result<TestResult> testGateway() {
        return Result.ok(service.testGateway(service.currentTenant()));
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank() || "null".equals(s)) ? null : s;
    }

    public record ProviderReq(String vendor, String name, List<String> modalities,
                              boolean enabled, Integer sort, Map<String, Object> values,
                              List<String> models) {}

    public record SystemModelReq(String providerId, String model) {}

    public record GatewayReq(boolean enabled, String gatewayType, String baseUrl, String token,
                             String defaultGroup, String modelMapping) {}
}
