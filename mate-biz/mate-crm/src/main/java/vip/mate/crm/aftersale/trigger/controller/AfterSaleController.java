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
package vip.mate.crm.aftersale.trigger.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
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
import vip.mate.base.result.PageResult;
import vip.mate.base.result.Result;
import vip.mate.crm.aftersale.application.command.AfterSaleCommand;
import vip.mate.crm.aftersale.application.command.AfterSaleCommandService;
import vip.mate.crm.aftersale.application.query.AfterSaleQueryService;
import vip.mate.crm.aftersale.domain.model.entity.AfterSale;
import vip.mate.crm.query.dto.AdvancedQuery;

/**
 * 售后工单 REST 入口(/api/v1/crm/aftersales): 受理/处理/完成/关闭/评价。
 *
 * @author mateaix
 */
@RestController
@RequestMapping("/api/v1/crm/aftersales")
@RequiredArgsConstructor
@SaCheckLogin
public class AfterSaleController {

    private final AfterSaleCommandService commandService;
    private final AfterSaleQueryService queryService;

    @GetMapping
    @SaCheckPermission("crm:aftersale:list")
    public Result<PageResult<AfterSale>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) Boolean mineOnly,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(queryService.page(keyword, status, type, customerId, mineOnly, pageNum, pageSize));
    }

    /** 条件查询(动态高级筛选)。 */
    @PostMapping("/search")
    @SaCheckPermission("crm:aftersale:list")
    public Result<PageResult<AfterSale>> search(@RequestBody AdvancedQuery query) {
        return Result.ok(queryService.search(query));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("crm:aftersale:list")
    public Result<AfterSaleQueryService.AfterSaleDetail> detail(@PathVariable String id) {
        return Result.ok(queryService.detail(id));
    }

    @PostMapping
    @SaCheckPermission("crm:aftersale:add")
    public Result<String> create(@RequestBody AfterSaleCommand cmd) {
        return Result.ok(commandService.create(cmd));
    }

    @PutMapping("/{id}/accept")
    @SaCheckPermission("crm:aftersale:handle")
    public Result<Void> accept(@PathVariable String id) {
        commandService.accept(id);
        return Result.ok();
    }

    @PutMapping("/{id}/process")
    @SaCheckPermission("crm:aftersale:handle")
    public Result<Void> process(@PathVariable String id, @RequestParam(required = false) String content) {
        commandService.process(id, content);
        return Result.ok();
    }

    @PutMapping("/{id}/resolve")
    @SaCheckPermission("crm:aftersale:handle")
    public Result<Void> resolve(@PathVariable String id, @RequestParam(required = false) String content) {
        commandService.resolve(id, content);
        return Result.ok();
    }

    @PutMapping("/{id}/close")
    @SaCheckPermission("crm:aftersale:close")
    public Result<Void> close(@PathVariable String id, @RequestParam(required = false) String content) {
        commandService.close(id, content);
        return Result.ok();
    }

    @PutMapping("/{id}/rate")
    @SaCheckPermission("crm:aftersale:handle")
    public Result<Void> rate(@PathVariable String id, @RequestParam Integer rating,
                             @RequestParam(required = false) String remark) {
        commandService.rate(id, rating, remark);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("crm:aftersale:close")
    public Result<Void> delete(@PathVariable String id) {
        commandService.delete(id);
        return Result.ok();
    }
}
