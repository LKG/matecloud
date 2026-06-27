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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vip.mate.base.result.PageResult;
import vip.mate.base.result.Result;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import vip.mate.starter.file.core.FileTemplate;
import vip.mate.starter.file.model.FileObject;
import vip.mate.starter.file.model.MultipartUpload;
import vip.mate.starter.file.model.UploadResult;

/**
 * Storage management — wraps the vendor-neutral {@link FileTemplate} as a REST
 * surface for the admin UI. Works with whatever provider {@code mate.file.type}
 * selects (MinIO/OSS/S3/…). Implements RFC-050 G2 (file management).
 *
 * @author mateaix
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/storage")
@RequiredArgsConstructor
public class StorageController {

    private static final int MAX_LIST_PAGE = 200;

    private final FileTemplate fileTemplate;

    /**
     * List files with simple offset/limit paging. Object stores list as a stream,
     * so we materialize up to {@code pageNum * pageSize + pageSize} entries and
     * slice client-side — adequate for an admin console.
     */
    @GetMapping
    public Result<PageResult<FileItem>> list(
            @RequestParam(required = false) String bucket,
            @RequestParam(required = false) String prefix,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {

        int safeSize = Math.min(Math.max(pageSize, 1), MAX_LIST_PAGE);
        int hardCap = safeSize * pageNum + safeSize;
        List<FileItem> all;
        try {
            all = fileTemplate.list(bucket, prefix, hardCap).stream()
                    .map(this::toItem)
                    .toList();
        } catch (Exception e) {
            log.warn("[storage] list failed: {}", e.getMessage());
            return Result.fail("Storage listing failed: " + e.getMessage());
        }

        int from = Math.min((pageNum - 1) * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());
        return Result.ok(PageResult.of(all.subList(from, to), (long) all.size()));
    }

    @PostMapping("/upload")
    public Result<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String bucket) {
        UploadResult r = (bucket == null || bucket.isBlank())
                ? fileTemplate.upload(file)
                : fileTemplate.upload(bucket, file);
        return Result.ok(Map.of(
                "objectName", r.objectName(),
                "url", r.url()));
    }

    @DeleteMapping("/{objectName}")
    public Result<Void> delete(
            @PathVariable String objectName,
            @RequestParam(required = false) String bucket) {
        fileTemplate.delete(bucket, objectName);
        return Result.ok();
    }

    @GetMapping("/{objectName}/url")
    public Result<Map<String, String>> presignedUrl(
            @PathVariable String objectName,
            @RequestParam(required = false) String bucket,
            @RequestParam(defaultValue = "3600") int expirySeconds) {
        String url = fileTemplate.presignedUrl(bucket, objectName, Duration.ofSeconds(expirySeconds));
        return Result.ok(Map.of("url", url));
    }

    // ---- multipart (chunked) upload ----
    // Flow: init → (front-end PUTs each chunk to its pre-signed URL) → complete.
    // Large files (video/courseware) go browser → object store directly, never
    // through this service. An interrupted upload resumes by re-requesting URLs
    // for the missing parts, then completing.

    /** Initiate a chunked upload; returns the bucket/objectName/uploadId handle. */
    @PostMapping("/multipart/init")
    public Result<MultipartUpload> initMultipart(
            @RequestParam(required = false) String bucket,
            @RequestParam(required = false) String objectName,
            @RequestParam(required = false) String contentType) {
        if (!fileTemplate.supportsMultipart()) {
            return Result.fail("The active storage provider does not support multipart upload");
        }
        return Result.ok(fileTemplate.initMultipartUpload(bucket, objectName, contentType));
    }

    /** Pre-signed PUT URL for a single part ({@code partNumber} is 1-based). */
    @GetMapping("/multipart/part-url")
    public Result<Map<String, String>> partUrl(
            @RequestParam String objectName,
            @RequestParam String uploadId,
            @RequestParam int partNumber,
            @RequestParam(required = false) String bucket,
            @RequestParam(defaultValue = "3600") int expirySeconds) {
        String url = fileTemplate.presignedPartUrl(
                bucket, objectName, uploadId, partNumber, Duration.ofSeconds(expirySeconds));
        return Result.ok(Map.of("url", url));
    }

    /** Merge the uploaded parts into the final object. */
    @PostMapping("/multipart/complete")
    public Result<Map<String, String>> completeMultipart(
            @RequestParam String objectName,
            @RequestParam String uploadId,
            @RequestParam(required = false) String bucket) {
        UploadResult r = fileTemplate.completeMultipartUpload(bucket, objectName, uploadId);
        return Result.ok(Map.of(
                "objectName", r.objectName(),
                "url", r.url()));
    }

    /** Abort an upload and discard the partial chunks. */
    @DeleteMapping("/multipart")
    public Result<Void> abortMultipart(
            @RequestParam String objectName,
            @RequestParam String uploadId,
            @RequestParam(required = false) String bucket) {
        fileTemplate.abortMultipartUpload(bucket, objectName, uploadId);
        return Result.ok();
    }

    private FileItem toItem(FileObject o) {
        return new FileItem(o.objectName(), o.size(), o.lastModified(), o.etag());
    }

    /** DTO returned to the UI. */
    public record FileItem(String objectName, long size, String lastModified, String etag) {}
}
