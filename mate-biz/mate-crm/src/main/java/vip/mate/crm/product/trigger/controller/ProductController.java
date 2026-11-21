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
package vip.mate.crm.product.trigger.controller;

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
import vip.mate.crm.product.application.command.ProductCommand;
import vip.mate.crm.product.application.command.ProductCommandService;
import vip.mate.crm.product.application.query.ProductQueryService;
import vip.mate.crm.product.domain.model.entity.Product;

import java.util.List;

/**
 * 产品 REST 入口(/api/v1/crm/products)。
 *
 * @author mateaix
 */
@RestController
@RequestMapping("/api/v1/crm/products")
@RequiredArgsConstructor
@SaCheckLogin
public class ProductController {

    private final ProductCommandService commandService;
    private final ProductQueryService queryService;

    @GetMapping
    @SaCheckPermission("crm:product:list")
    public Result<PageResult<Product>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(queryService.page(keyword, category, status, pageNum, pageSize));
    }

    @GetMapping("/on-sale")
    @SaCheckPermission("crm:product:list")
    public Result<List<Product>> onSale() {
        return Result.ok(queryService.listOnSale());
    }

    @GetMapping("/{id}")
    @SaCheckPermission("crm:product:list")
    public Result<Product> getById(@PathVariable String id) {
        Product product = queryService.findById(id);
        return product == null ? Result.fail("产品不存在") : Result.ok(product);
    }

    @PostMapping
    @SaCheckPermission("crm:product:add")
    public Result<String> create(@RequestBody ProductCommand cmd) {
        return Result.ok(commandService.create(cmd));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("crm:product:edit")
    public Result<Void> update(@PathVariable String id, @RequestBody ProductCommand cmd) {
        commandService.update(id, cmd);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    @SaCheckPermission("crm:product:edit")
    public Result<Void> switchStatus(@PathVariable String id, @RequestParam int status) {
        commandService.switchStatus(id, status);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("crm:product:delete")
    public Result<Void> delete(@PathVariable String id) {
        commandService.delete(id);
        return Result.ok();
    }
}
