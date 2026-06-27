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
package vip.mate.starter.file.provider;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.multipart.MultipartFile;
import vip.mate.base.channel.ChannelConfigStore;
import vip.mate.base.channel.ProviderDescriptor;
import vip.mate.starter.file.config.MinioProperties;
import vip.mate.starter.file.annotation.FileProvider;
import vip.mate.starter.file.core.FileStorageException;
import vip.mate.starter.file.model.FileObject;
import vip.mate.starter.file.model.MultipartUpload;
import vip.mate.starter.file.model.UploadCommand;
import vip.mate.starter.file.model.UploadResult;
import vip.mate.starter.file.spi.AbstractFileStorage;

import java.io.InputStream;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * MinIO / S3-compatible {@link vip.mate.starter.file.spi.FileStorage} provider
 * (default), backed entirely by AWS SDK v2 via {@link S3StorageClient}.
 *
 * <p>Effective config = admin-managed config (from {@link ChannelConfigStore})
 * merged over the static {@link MinioProperties}, so editing it in the console
 * takes effect without a restart. Exactly one {@link S3StorageClient} is kept for
 * the active endpoint/credentials; when they change, the previous client is
 * closed before the new one is built — no leaked connections on rotation.
 *
 * @author mateaix
 */
@FileProvider(value = "minio", describe = "MinIO / S3-compatible object storage")
public class MinioFileStorage extends AbstractFileStorage {

    private static final Logger log = LoggerFactory.getLogger(MinioFileStorage.class);

    /** Pre-signed GET URL cap (7 days = S3 signature-v4 maximum). */
    private static final long PRESIGN_GET_MAX_SECONDS = TimeUnit.DAYS.toSeconds(7);
    /** Pre-signed part URL validity — wide enough for a slow chunk on a flaky link. */
    private static final long PART_URL_EXPIRY_SECONDS = TimeUnit.HOURS.toSeconds(1);

    private final MinioProperties properties;
    private final ObjectProvider<ChannelConfigStore> configStore;

    // Single active client; rebuilt + old one closed when the effective config changes.
    private final Object clientLock = new Object();
    private volatile String activeKey;
    private volatile S3StorageClient activeClient;

    public MinioFileStorage(MinioProperties properties, ObjectProvider<ChannelConfigStore> configStore) {
        this.properties = properties;
        this.configStore = configStore;
    }

    @Override
    public ProviderDescriptor descriptor() {
        return ProviderDescriptor.builder("minio", "MinIO")
                .describe("S3 兼容对象存储(默认)")
                .text("endpoint", "Endpoint", true, "http://127.0.0.1:9000")
                .text("accessKey", "AccessKey", true)
                .secret("secretKey", "SecretKey", true)
                .text("bucketName", "Bucket", true, "matecloud")
                .build();
    }

    private Map<String, String> cfg() {
        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put("endpoint", nz(properties.getEndpoint()));
        defaults.put("accessKey", nz(properties.getAccessKey()));
        defaults.put("secretKey", nz(properties.getSecretKey()));
        defaults.put("bucketName", nz(properties.getBucketName()));
        return effectiveConfig(configStore == null ? null : configStore.getIfAvailable(), defaults);
    }

    /** The active client; rebuilt (and the old one closed) when credentials/endpoint change. */
    private S3StorageClient client(Map<String, String> cfg) {
        String ep = cfg.get("endpoint"), ak = cfg.get("accessKey"), sk = cfg.get("secretKey");
        String key = ep + "|" + ak + "|" + sk;
        S3StorageClient current = activeClient;
        if (current != null && key.equals(activeKey)) {
            return current;
        }
        synchronized (clientLock) {
            if (activeClient != null && key.equals(activeKey)) {
                return activeClient;
            }
            S3StorageClient old = activeClient;
            S3StorageClient fresh = new S3StorageClient(ep, ak, sk);
            activeClient = fresh;
            activeKey = key;
            if (old != null) {
                old.close(); // rotate: release the previous client's connections
            }
            return fresh;
        }
    }

    @PreDestroy
    void shutdown() {
        synchronized (clientLock) {
            if (activeClient != null) {
                activeClient.close();
                activeClient = null;
            }
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String bucketOr(Map<String, String> cfg, String bucket) {
        return (bucket == null || bucket.isBlank()) ? cfg.get("bucketName") : bucket;
    }

    @Override
    public String defaultBucket() {
        return cfg().get("bucketName");
    }

    @Override
    public UploadResult upload(UploadCommand command) {
        Map<String, String> cfg = cfg();
        S3StorageClient s3 = client(cfg);
        String bucket = bucketOr(cfg, command.getBucket());
        String objectName = resolveObjectName(command);
        s3.ensureBucket(bucket);
        try {
            InputStream in;
            long size;
            String contentType;
            MultipartFile file = command.getMultipartFile();
            if (file != null) {
                in = file.getInputStream();
                size = file.getSize();
                contentType = sanitizeContentType(file.getContentType());
            } else {
                in = command.getInputStream();
                size = command.getSize();
                contentType = sanitizeContentType(command.getContentType());
            }
            if (in == null) {
                throw new FileStorageException("UploadCommand has neither a MultipartFile nor an InputStream");
            }
            s3.putObject(bucket, objectName, in, size, contentType);
            log.info("Uploaded: bucket={}, object={}", bucket, objectName);
            return new UploadResult(objectName, bucket, publicUrl(cfg, bucket, objectName),
                    size, command.getOriginalFilename(), type());
        } catch (FileStorageException e) {
            throw e;
        } catch (Exception e) {
            throw new FileStorageException("Failed to upload: " + objectName, e);
        }
    }

    @Override
    public InputStream download(String bucket, String objectName) {
        Map<String, String> cfg = cfg();
        return client(cfg).getObject(bucketOr(cfg, bucket), objectName);
    }

    @Override
    public void delete(String bucket, String objectName) {
        Map<String, String> cfg = cfg();
        String b = bucketOr(cfg, bucket);
        client(cfg).deleteObject(b, objectName);
        log.info("Deleted: bucket={}, object={}", b, objectName);
    }

    @Override
    public String publicUrl(String bucket, String objectName) {
        return publicUrl(cfg(), bucket, objectName);
    }

    private String publicUrl(Map<String, String> cfg, String bucket, String objectName) {
        return cfg.get("endpoint") + "/" + bucketOr(cfg, bucket) + "/" + objectName;
    }

    @Override
    public String presignedUrl(String bucket, String objectName, Duration expiry) {
        Map<String, String> cfg = cfg();
        long seconds = Math.max(1, Math.min(expiry.getSeconds(), PRESIGN_GET_MAX_SECONDS));
        return client(cfg).presignGet(bucketOr(cfg, bucket), objectName, Duration.ofSeconds(seconds));
    }

    @Override
    public List<FileObject> list(String bucket, String prefix, int limit) {
        Map<String, String> cfg = cfg();
        return client(cfg).list(bucketOr(cfg, bucket), prefix, limit);
    }

    // ---- multipart (chunked) upload ----

    @Override
    public boolean supportsMultipart() {
        return true;
    }

    @Override
    public MultipartUpload initMultipartUpload(String bucket, String objectName, String contentType) {
        Map<String, String> cfg = cfg();
        S3StorageClient s3 = client(cfg);
        String b = bucketOr(cfg, bucket);
        String object = (objectName == null || objectName.isBlank())
                ? generateObjectName(objectName) : objectName;
        s3.ensureBucket(b);
        String uploadId = s3.createUploadId(b, object, sanitizeContentType(contentType));
        log.info("Init multipart upload: bucket={}, object={}, uploadId={}", b, object, uploadId);
        return new MultipartUpload(b, object, uploadId);
    }

    @Override
    public String presignedPartUrl(String bucket, String objectName, String uploadId,
                                   int partNumber, Duration expiry) {
        if (partNumber < 1) {
            throw new FileStorageException("partNumber must be >= 1, was " + partNumber);
        }
        Map<String, String> cfg = cfg();
        long seconds = expiry == null ? PART_URL_EXPIRY_SECONDS
                : Math.max(1, Math.min(expiry.getSeconds(), PART_URL_EXPIRY_SECONDS));
        return client(cfg).presignPartUrl(
                bucketOr(cfg, bucket), objectName, uploadId, partNumber, Duration.ofSeconds(seconds));
    }

    @Override
    public UploadResult completeMultipartUpload(String bucket, String objectName, String uploadId) {
        Map<String, String> cfg = cfg();
        String b = bucketOr(cfg, bucket);
        long size = client(cfg).complete(b, objectName, uploadId);
        log.info("Completed multipart upload: bucket={}, object={}, size={}", b, objectName, size);
        return new UploadResult(objectName, b, publicUrl(cfg, b, objectName), size, objectName, type());
    }

    @Override
    public void abortMultipartUpload(String bucket, String objectName, String uploadId) {
        Map<String, String> cfg = cfg();
        String b = bucketOr(cfg, bucket);
        client(cfg).abort(b, objectName, uploadId);
        log.info("Aborted multipart upload: bucket={}, object={}, uploadId={}", b, objectName, uploadId);
    }
}
