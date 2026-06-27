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
package vip.mate.ai.trigger.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vip.mate.ai.application.command.ProviderCommandService;
import vip.mate.ai.application.query.IProviderQueryService;
import vip.mate.ai.application.query.IProviderQueryService.ProviderView;
import vip.mate.ai.domain.model.aggregate.ProviderAggregate;
import vip.mate.base.result.PageResult;
import vip.mate.base.result.Result;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ai/providers")
@RequiredArgsConstructor
public class ProviderController {

    private final ProviderCommandService providerCommandService;
    private final IProviderQueryService providerQueryService;

    /**
     * Create / update payload. {@code apiKey} is plaintext on the wire;
     * it is encrypted server-side before persistence and never returned.
     */
    @Data
    public static class ProviderRequest {
        private String code;
        private String name;
        private String vendor;
        private String baseUrl;
        private String apiKey;
        private String defaultModel;
        private String availableModels;
        private BigDecimal temperature;
        private Integer maxTokens;
        private Integer enabled;
        private Integer isDefault;
        private Integer sort;
    }

    @GetMapping
    public Result<PageResult<ProviderView>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String vendor,
            @RequestParam(required = false) Integer enabled) {
        return Result.ok(providerQueryService.page(pageNum, pageSize, keyword, vendor, enabled));
    }

    @GetMapping("/enabled")
    public Result<List<ProviderView>> listEnabled() {
        return Result.ok(providerQueryService.listEnabled());
    }

    @GetMapping("/{id}")
    public Result<ProviderView> detail(@PathVariable String id) {
        return Result.ok(providerQueryService.detail(id));
    }

    @PostMapping
    public Result<String> create(@RequestBody ProviderRequest req) {
        return Result.ok(providerCommandService.create(toAggregate(req), req.getApiKey()));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody ProviderRequest req) {
        providerCommandService.update(id, toAggregate(req), req.getApiKey());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        providerCommandService.delete(id);
        return Result.ok();
    }

    @PutMapping("/{id}/enabled")
    public Result<Void> toggle(@PathVariable String id, @RequestParam boolean enabled) {
        providerCommandService.toggleEnabled(id, enabled);
        return Result.ok();
    }

    @PostMapping("/{id}/test")
    public Result<Boolean> test(@PathVariable String id) {
        return Result.ok(providerCommandService.test(id));
    }

    private ProviderAggregate toAggregate(ProviderRequest req) {
        return ProviderAggregate.builder()
                .code(req.getCode())
                .name(req.getName())
                .vendor(req.getVendor())
                .baseUrl(req.getBaseUrl())
                .defaultModel(req.getDefaultModel())
                .availableModels(req.getAvailableModels())
                .temperature(req.getTemperature())
                .maxTokens(req.getMaxTokens())
                .enabled(req.getEnabled())
                .isDefault(req.getIsDefault())
                .sort(req.getSort())
                .build();
    }
}
