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
package vip.mate.crm.trade.trigger.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
import vip.mate.crm.query.dto.AdvancedQuery;
import vip.mate.crm.trade.application.command.InvoiceCommand;
import vip.mate.crm.trade.application.command.InvoiceCommandService;
import vip.mate.crm.trade.application.query.InvoiceQueryService;
import vip.mate.crm.trade.domain.model.entity.Invoice;

import java.time.LocalDate;

/**
 * 发票 REST 入口(/api/v1/crm/invoices): 开票申请 → 开票/驳回 → 作废。
 *
 * @author mateaix
 */
@RestController
@RequestMapping("/api/v1/crm/invoices")
@RequiredArgsConstructor
@SaCheckLogin
public class InvoiceController {

    private final InvoiceCommandService commandService;
    private final InvoiceQueryService queryService;

    @GetMapping
    @SaCheckPermission("crm:invoice:list")
    public Result<PageResult<Invoice>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String contractId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Boolean mineOnly,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(queryService.page(keyword, contractId, status, mineOnly, pageNum, pageSize));
    }

    /** 条件查询(动态高级筛选)。 */
    @PostMapping("/search")
    @SaCheckPermission("crm:invoice:list")
    public Result<PageResult<Invoice>> search(@RequestBody AdvancedQuery query) {
        return Result.ok(queryService.search(query));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("crm:invoice:list")
    public Result<Invoice> getById(@PathVariable String id) {
        Invoice invoice = queryService.findById(id);
        return invoice == null ? Result.fail("发票不存在") : Result.ok(invoice);
    }

    @PostMapping
    @SaCheckPermission("crm:invoice:add")
    public Result<String> apply(@RequestBody InvoiceCommand cmd) {
        return Result.ok(commandService.apply(cmd));
    }

    @PutMapping("/{id}/issue")
    @SaCheckPermission("crm:invoice:issue")
    public Result<Void> issue(@PathVariable String id,
                              @RequestParam(required = false) String invoiceNo,
                              @RequestParam(required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate invoiceDate) {
        commandService.issue(id, invoiceNo, invoiceDate);
        return Result.ok();
    }

    @PutMapping("/{id}/reject")
    @SaCheckPermission("crm:invoice:issue")
    public Result<Void> reject(@PathVariable String id) {
        commandService.reject(id);
        return Result.ok();
    }

    @PutMapping("/{id}/void")
    @SaCheckPermission("crm:invoice:void")
    public Result<Void> voidInvoice(@PathVariable String id) {
        commandService.voidInvoice(id);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("crm:invoice:void")
    public Result<Void> delete(@PathVariable String id) {
        commandService.delete(id);
        return Result.ok();
    }
}
