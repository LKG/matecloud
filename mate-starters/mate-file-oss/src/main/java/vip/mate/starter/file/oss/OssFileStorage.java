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
package vip.mate.starter.file.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.multipart.MultipartFile;
import vip.mate.base.channel.ChannelConfigStore;
import vip.mate.base.channel.ProviderDescriptor;
import vip.mate.starter.file.annotation.FileProvider;
import vip.mate.starter.file.core.FileStorageException;
import vip.mate.starter.file.model.FileObject;
import vip.mate.starter.file.model.UploadCommand;
import vip.mate.starter.file.model.UploadResult;
import vip.mate.starter.file.spi.AbstractFileStorage;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aliyun OSS-backed {@link vip.mate.starter.file.spi.FileStorage} provider.
 *
 * <p>Registered unconditionally so it appears in the admin "渠道配置" list; its
 * effective config = admin-managed (from {@link ChannelConfigStore}) over static
 * {@link OssProperties}. The OSS client is built from the effective endpoint/keys
 * and cached, so editing config in the console takes effect without a restart.
 *
 * @author mateaix
 */
@FileProvider(value = "oss", describe = "Aliyun OSS object storage")
public class OssFileStorage extends AbstractFileStorage implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(OssFileStorage.class);

    private final OssProperties properties;
    private final ObjectProvider<ChannelConfigStore> configStore;
    private final Map<String, OSS> clientCache = new ConcurrentHashMap<>();

    public OssFileStorage(OssProperties properties, ObjectProvider<ChannelConfigStore> configStore) {
        this.properties = properties;
        this.configStore = configStore;
    }

    @Override
    public ProviderDescriptor descriptor() {
        return ProviderDescriptor.builder("oss", "阿里云 OSS")
                .describe("Aliyun Object Storage Service")
                .text("endpoint", "Endpoint", true, "https://oss-cn-hangzhou.aliyuncs.com")
                .text("accessKeyId", "AccessKeyId", true)
                .secret("accessKeySecret", "AccessKeySecret", true)
                .text("bucketName", "Bucket", true)
                .text("domain", "访问域名", false)
                .build();
    }

    private Map<String, String> cfg() {
        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put("endpoint", nz(properties.getEndpoint()));
        defaults.put("accessKeyId", nz(properties.getAccessKey()));
        defaults.put("accessKeySecret", nz(properties.getSecretKey()));
        defaults.put("bucketName", nz(properties.getBucketName()));
        defaults.put("domain", nz(properties.getDomain()));
        return effectiveConfig(configStore == null ? null : configStore.getIfAvailable(), defaults);
    }

    private OSS client(Map<String, String> cfg) {
        String ak = cfg.get("accessKeyId"), sk = cfg.get("accessKeySecret");
        String ep = normalizeEndpoint(cfg.get("endpoint"), cfg.get("bucketName"));
        if (ep == null || ep.isBlank() || ak == null || ak.isBlank()) {
            throw new FileStorageException("OSS not configured (endpoint/accessKeyId missing)");
        }
        final String fep = ep;
        return clientCache.computeIfAbsent(ep + "|" + ak + "|" + sk,
                k -> new OSSClientBuilder().build(fep, ak, sk));
    }

    /**
     * Normalize an OSS endpoint to the region endpoint the SDK expects.
     * <p>
     * The Aliyun SDK uses virtual-host addressing (it prepends {@code bucket.}
     * to the endpoint host itself), so the endpoint must be the REGION node
     * (e.g. {@code https://oss-cn-zhangjiakou.aliyuncs.com}). A very common
     * mis-configuration is pasting the bucket's access domain
     * ({@code bucket.oss-cn-...aliyuncs.com}); that makes the SDK build
     * {@code bucket.bucket.oss-...} → {@code InvalidBucketName}. We defensively
     * (1) add an https:// scheme when missing and (2) strip a leading
     * {@code <bucket>.} prefix from the host.
     */
    static String normalizeEndpoint(String endpoint, String bucket) {
        if (endpoint == null) {
            return null;
        }
        String ep = endpoint.trim();
        if (ep.isEmpty()) {
            return ep;
        }
        String scheme = "https://";
        String host = ep;
        if (ep.regionMatches(true, 0, "http://", 0, 7)) {
            scheme = "http://";
            host = ep.substring(7);
        } else if (ep.regionMatches(true, 0, "https://", 0, 8)) {
            host = ep.substring(8);
        }
        while (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }
        if (bucket != null && !bucket.isBlank()) {
            String prefix = bucket + ".";
            if (host.regionMatches(true, 0, prefix, 0, prefix.length())) {
                host = host.substring(prefix.length());
            }
        }
        return scheme + host;
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
        String bucket = bucketOr(cfg, command.getBucket());
        String objectName = resolveObjectName(command);
        if (bucket == null || bucket.isBlank()) {
            throw new FileStorageException("OSS bucket not configured (set Bucket in 渠道配置)");
        }
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
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            if (size >= 0) {
                metadata.setContentLength(size);
            }
            client(cfg).putObject(new PutObjectRequest(bucket, objectName, in, metadata));
            log.info("Uploaded to OSS: bucket={}, object={}", bucket, objectName);
            return new UploadResult(objectName, bucket, publicUrl(cfg, bucket, objectName),
                    size, command.getOriginalFilename(), type());
        } catch (FileStorageException e) {
            throw e;
        } catch (Exception e) {
            throw new FileStorageException("Failed to upload to OSS: " + objectName
                    + " (bucket=" + bucket + ", endpoint=" + cfg.get("endpoint") + ")", e);
        }
    }

    @Override
    public InputStream download(String bucket, String objectName) {
        Map<String, String> cfg = cfg();
        try {
            return client(cfg).getObject(bucketOr(cfg, bucket), objectName).getObjectContent();
        } catch (Exception e) {
            throw new FileStorageException("Failed to download from OSS: " + objectName, e);
        }
    }

    @Override
    public void delete(String bucket, String objectName) {
        Map<String, String> cfg = cfg();
        try {
            client(cfg).deleteObject(bucketOr(cfg, bucket), objectName);
            log.info("Deleted from OSS: bucket={}, object={}", bucketOr(cfg, bucket), objectName);
        } catch (Exception e) {
            throw new FileStorageException("Failed to delete from OSS: " + objectName, e);
        }
    }

    @Override
    public String publicUrl(String bucket, String objectName) {
        return publicUrl(cfg(), bucket, objectName);
    }

    private String publicUrl(Map<String, String> cfg, String bucket, String objectName) {
        String domain = cfg.get("domain");
        if (domain != null && !domain.isBlank()) {
            return domain + "/" + objectName;
        }
        String host = nz(normalizeEndpoint(cfg.get("endpoint"), cfg.get("bucketName")))
                .replaceFirst("^https?://", "");
        return "https://" + bucketOr(cfg, bucket) + "." + host + "/" + objectName;
    }

    @Override
    public String presignedUrl(String bucket, String objectName, Duration expiry) {
        Map<String, String> cfg = cfg();
        try {
            long seconds = Math.max(1, Math.min(expiry.getSeconds(), Duration.ofDays(7).getSeconds()));
            Date expiration = Date.from(Instant.now().plusSeconds(seconds));
            return client(cfg).generatePresignedUrl(bucketOr(cfg, bucket), objectName, expiration).toString();
        } catch (Exception e) {
            throw new FileStorageException("Failed to presign OSS url: " + objectName, e);
        }
    }

    @Override
    public List<FileObject> list(String bucket, String prefix, int limit) {
        Map<String, String> cfg = cfg();
        try {
            ListObjectsRequest request = new ListObjectsRequest(bucketOr(cfg, bucket));
            if (prefix != null && !prefix.isBlank()) {
                request.setPrefix(prefix);
            }
            if (limit > 0) {
                request.setMaxKeys(Math.min(limit, 1000));
            }
            ObjectListing listing = client(cfg).listObjects(request);
            List<FileObject> out = new ArrayList<>();
            for (OSSObjectSummary s : listing.getObjectSummaries()) {
                out.add(new FileObject(
                        s.getKey(),
                        s.getSize(),
                        s.getLastModified() == null ? null : s.getLastModified().toInstant().toString(),
                        s.getETag()));
                if (limit > 0 && out.size() >= limit) {
                    break;
                }
            }
            return out;
        } catch (Exception e) {
            throw new FileStorageException("Failed to list OSS objects", e);
        }
    }

    @Override
    public void destroy() {
        clientCache.values().forEach(c -> {
            try {
                c.shutdown();
            } catch (Exception ignored) {
                // best-effort
            }
        });
        clientCache.clear();
    }
}
