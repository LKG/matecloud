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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vip.mate.base.result.PageResult;
import vip.mate.base.result.Result;
import vip.mate.system.admin.application.material.MaterialService;
import vip.mate.system.admin.application.material.MaterialService.CategoryListVO;
import vip.mate.system.admin.application.material.MaterialService.MaterialVO;
import vip.mate.system.admin.application.material.MaterialService.MultipartInitVO;
import vip.mate.system.admin.trigger.annotation.OperationLog;

import java.util.Map;

/**
 * 素材管理 REST。素材内容存对象存储,仅元数据落库;按 type(image/video/doc)与分组组织。
 *
 * @author mateaix
 */
@RestController
@RequestMapping("/api/v1/admin/material")
@RequiredArgsConstructor
@SaCheckLogin
public class MaterialController {

    private final MaterialService service;

    // ---- 分组 ----

    @GetMapping("/categories")
    public Result<CategoryListVO> categories(@RequestParam String type) {
        return Result.ok(service.categories(type));
    }

    @PostMapping("/categories")
    @OperationLog(module = "素材管理", type = "新增", description = "新建素材分组")
    public Result<String> createCategory(@RequestBody CategoryReq req) {
        return Result.ok(service.createCategory(req.type(), req.name()));
    }

    @PutMapping("/categories/{id}")
    @OperationLog(module = "素材管理", type = "修改", description = "重命名分组")
    public Result<Void> renameCategory(@PathVariable String id, @RequestBody CategoryReq req) {
        service.renameCategory(id, req.name());
        return Result.ok();
    }

    @DeleteMapping("/categories/{id}")
    @OperationLog(module = "素材管理", type = "删除", description = "删除分组")
    public Result<Void> deleteCategory(@PathVariable String id) {
        service.deleteCategory(id);
        return Result.ok();
    }

    // ---- 素材 ----

    @GetMapping
    public Result<PageResult<MaterialVO>> page(
            @RequestParam String type,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "24") int pageSize) {
        return Result.ok(service.page(type, categoryId, keyword, pageNum, pageSize));
    }

    @PostMapping("/upload")
    @OperationLog(module = "素材管理", type = "上传", description = "上传素材")
    public Result<MaterialVO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam String type,
            @RequestParam(required = false) String categoryId) {
        return Result.ok(service.upload(file, type, categoryId));
    }

    // ---- 分片上传(大文件)----
    // 流程:init → 前端逐片 PUT 到预签名 URL(浏览器直传对象存储)→ complete 合并并落库。
    // 中断可只补缺片再 complete;放弃则 abort。

    @PostMapping("/upload/multipart/init")
    @OperationLog(module = "素材管理", type = "上传", description = "初始化分片上传")
    public Result<MultipartInitVO> initMultipart(@RequestBody MultipartInitReq req) {
        return Result.ok(service.initMultipart(req.type(), req.filename(), req.contentType(), req.size()));
    }

    @GetMapping("/upload/multipart/part-url")
    public Result<Map<String, String>> partUrl(
            @RequestParam String objectName,
            @RequestParam String uploadId,
            @RequestParam int partNumber,
            @RequestParam(defaultValue = "3600") int expirySeconds) {
        return Result.ok(Map.of("url",
                service.partUrl(objectName, uploadId, partNumber, expirySeconds)));
    }

    @PostMapping("/upload/multipart/complete")
    @OperationLog(module = "素材管理", type = "上传", description = "完成分片上传")
    public Result<MaterialVO> completeMultipart(@RequestBody MultipartCompleteReq req) {
        return Result.ok(service.completeMultipart(req.type(), req.categoryId(), req.objectName(),
                req.uploadId(), req.filename(), req.size(), req.contentType()));
    }

    @DeleteMapping("/upload/multipart")
    public Result<Void> abortMultipart(
            @RequestParam String objectName,
            @RequestParam String uploadId) {
        service.abortMultipart(objectName, uploadId);
        return Result.ok();
    }

    @PutMapping("/{id}/rename")
    @OperationLog(module = "素材管理", type = "修改", description = "重命名素材")
    public Result<Void> rename(@PathVariable String id, @RequestBody NameReq req) {
        service.update(id, req.name(), null, false);
        return Result.ok();
    }

    @PutMapping("/{id}/move")
    @OperationLog(module = "素材管理", type = "修改", description = "移动素材分组")
    public Result<Void> move(@PathVariable String id, @RequestBody MoveReq req) {
        service.update(id, null, req.categoryId(), true);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "素材管理", type = "删除", description = "删除素材")
    public Result<Void> delete(@PathVariable String id) {
        service.delete(id);
        return Result.ok();
    }

    @GetMapping("/{id}/url")
    public Result<Map<String, String>> url(@PathVariable String id) {
        return Result.ok(Map.of("url", service.presignedUrl(id)));
    }

    public record CategoryReq(String type, String name) {}

    public record NameReq(String name) {}

    public record MoveReq(String categoryId) {}

    public record MultipartInitReq(String type, String filename, String contentType, long size) {}

    public record MultipartCompleteReq(String type, String categoryId, String objectName, String uploadId,
                                       String filename, long size, String contentType) {}
}
