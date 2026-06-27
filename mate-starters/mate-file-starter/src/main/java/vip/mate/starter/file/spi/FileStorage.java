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
package vip.mate.starter.file.spi;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import vip.mate.base.channel.ProviderDescriptor;
import vip.mate.starter.file.annotation.FileProvider;
import vip.mate.starter.file.core.FileStorageException;
import vip.mate.starter.file.core.FileTemplate;
import vip.mate.starter.file.model.FileObject;
import vip.mate.starter.file.model.MultipartUpload;
import vip.mate.starter.file.model.UploadCommand;
import vip.mate.starter.file.model.UploadResult;

/**
 * Object-storage SPI. Implement once per backend (MinIO, Aliyun OSS, AWS S3, …),
 * annotate the impl with {@link FileProvider} to declare its {@link #type()}, and
 * register it as a Spring bean. Application code never touches a vendor SDK —
 * it goes through {@link FileTemplate}, which selects the active provider.
 *
 * <p>A {@code null}/blank {@code bucket} argument means "use the provider's
 * configured default bucket".
 *
 * @author mateaix
 */
public interface FileStorage {

    /** Provider type key, e.g. {@code "minio"} (mirrors {@link FileProvider#value()}). */
    String type();

    /** Self-description (display name + config field metadata) for the admin UI. */
    ProviderDescriptor descriptor();

    /** The provider's configured default bucket. */
    String defaultBucket();

    /** Upload an object. */
    UploadResult upload(UploadCommand command);

    /** Open a stream to read an object. Caller must close it. */
    InputStream download(String bucket, String objectName);

    /** Delete an object (idempotent). */
    void delete(String bucket, String objectName);

    /** Build a (possibly public) direct access URL. */
    String publicUrl(String bucket, String objectName);

    /** Build a time-limited pre-signed GET URL. */
    String presignedUrl(String bucket, String objectName, Duration expiry);

    /** List up to {@code limit} objects under {@code prefix}. */
    List<FileObject> list(String bucket, String prefix, int limit);

    // ---- multipart (chunked) upload — direct browser → object store ----
    // Optional capability. Providers that don't support it inherit the
    // default-throwing methods below; call sites should gate on the backend.

    /**
     * Whether this provider supports the multipart (chunked) upload flow.
     * Default {@code false}; override to {@code true} when implemented.
     */
    default boolean supportsMultipart() {
        return false;
    }

    /**
     * Initiate a multipart upload. A blank {@code objectName} is generated.
     *
     * @return handle carrying the resolved bucket/objectName and the vendor upload id
     */
    default MultipartUpload initMultipartUpload(String bucket, String objectName, String contentType) {
        throw new FileStorageException(type() + " does not support multipart upload");
    }

    /**
     * Pre-signed PUT URL for a single part ({@code partNumber} is 1-based). The
     * client uploads the chunk's bytes directly to this URL.
     */
    default String presignedPartUrl(String bucket, String objectName, String uploadId,
                                    int partNumber, Duration expiry) {
        throw new FileStorageException(type() + " does not support multipart upload");
    }

    /**
     * Complete the upload: the provider lists the already-uploaded parts and merges
     * them into the final object.
     */
    default UploadResult completeMultipartUpload(String bucket, String objectName, String uploadId) {
        throw new FileStorageException(type() + " does not support multipart upload");
    }

    /** Abort an upload and free the partial chunks (idempotent). */
    default void abortMultipartUpload(String bucket, String objectName, String uploadId) {
        throw new FileStorageException(type() + " does not support multipart upload");
    }
}
