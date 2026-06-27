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
package vip.mate.starter.file.core;

import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import vip.mate.starter.file.model.FileObject;
import vip.mate.starter.file.model.MultipartUpload;
import vip.mate.starter.file.model.UploadCommand;
import vip.mate.starter.file.model.UploadResult;
import vip.mate.starter.file.spi.FileStorage;

/**
 * Application-facing facade for object storage. Resolves the active provider per
 * call (via {@link FileStorageResolver}) and delegates — callers never depend on
 * a vendor SDK.
 *
 * <pre>{@code
 *   UploadResult r = fileTemplate.upload(multipartFile);
 *   String url     = fileTemplate.presignedUrl(r.objectName(), Duration.ofHours(1));
 * }</pre>
 *
 * @author mateaix
 */
public class FileTemplate {

    private final FileStorageRegistry registry;
    private final FileStorageResolver resolver;

    public FileTemplate(FileStorageRegistry registry, FileStorageResolver resolver) {
        this.registry = registry;
        this.resolver = resolver;
    }

    /** The currently active provider (advanced use / provider-specific calls). */
    public FileStorage storage() {
        return registry.get(resolver.resolveType());
    }

    public UploadResult upload(MultipartFile file) {
        return storage().upload(UploadCommand.of(file));
    }

    public UploadResult upload(String bucket, MultipartFile file) {
        return storage().upload(UploadCommand.of(file).bucket(bucket));
    }

    public UploadResult upload(UploadCommand command) {
        return storage().upload(command);
    }

    public InputStream download(String objectName) {
        return storage().download(null, objectName);
    }

    public InputStream download(String bucket, String objectName) {
        return storage().download(bucket, objectName);
    }

    public void delete(String objectName) {
        storage().delete(null, objectName);
    }

    public void delete(String bucket, String objectName) {
        storage().delete(bucket, objectName);
    }

    public String publicUrl(String objectName) {
        return storage().publicUrl(null, objectName);
    }

    public String publicUrl(String bucket, String objectName) {
        return storage().publicUrl(bucket, objectName);
    }

    public String presignedUrl(String objectName, Duration expiry) {
        return storage().presignedUrl(null, objectName, expiry);
    }

    public String presignedUrl(String bucket, String objectName, Duration expiry) {
        return storage().presignedUrl(bucket, objectName, expiry);
    }

    public List<FileObject> list(String bucket, String prefix, int limit) {
        return storage().list(bucket, prefix, limit);
    }

    // ---- multipart (chunked) upload — see FileStorage for the flow ----

    /** Whether the active provider supports multipart upload. */
    public boolean supportsMultipart() {
        return storage().supportsMultipart();
    }

    public MultipartUpload initMultipartUpload(String objectName, String contentType) {
        return storage().initMultipartUpload(null, objectName, contentType);
    }

    public MultipartUpload initMultipartUpload(String bucket, String objectName, String contentType) {
        return storage().initMultipartUpload(bucket, objectName, contentType);
    }

    public String presignedPartUrl(String objectName, String uploadId, int partNumber, Duration expiry) {
        return storage().presignedPartUrl(null, objectName, uploadId, partNumber, expiry);
    }

    public String presignedPartUrl(String bucket, String objectName, String uploadId,
                                   int partNumber, Duration expiry) {
        return storage().presignedPartUrl(bucket, objectName, uploadId, partNumber, expiry);
    }

    public UploadResult completeMultipartUpload(String objectName, String uploadId) {
        return storage().completeMultipartUpload(null, objectName, uploadId);
    }

    public UploadResult completeMultipartUpload(String bucket, String objectName, String uploadId) {
        return storage().completeMultipartUpload(bucket, objectName, uploadId);
    }

    public void abortMultipartUpload(String objectName, String uploadId) {
        storage().abortMultipartUpload(null, objectName, uploadId);
    }

    public void abortMultipartUpload(String bucket, String objectName, String uploadId) {
        storage().abortMultipartUpload(bucket, objectName, uploadId);
    }
}
