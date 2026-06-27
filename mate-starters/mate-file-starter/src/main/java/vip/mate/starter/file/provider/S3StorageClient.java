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

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;
import software.amazon.awssdk.services.s3.model.ListPartsResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.Part;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;
import vip.mate.starter.file.core.FileStorageException;
import vip.mate.starter.file.model.FileObject;

/**
 * One S3-compatible client for <b>all</b> object-storage operations — single PUT,
 * GET, delete, list, pre-signed GET, and the multipart flow — on AWS SDK v2.
 *
 * <p>Unifies what previously needed two SDKs (MinIO for simple ops + AWS for
 * multipart) onto a single one: MinIO/OSS/AWS all speak S3, so path-style
 * addressing + {@code us-east-1} as the neutral default covers MinIO endpoints,
 * mirroring the reference design's "one S3 client for everything" approach.
 *
 * <p>{@link S3Client} and {@link S3Presigner} are thread-safe and long-lived;
 * the provider keeps one instance per active endpoint/credentials and
 * {@link #close() closes} it on rotation/shutdown to release connections.
 *
 * @author mateaix
 */
class S3StorageClient implements AutoCloseable {

    private static final int DEFAULT_LIST_MAX = 1000;

    private final S3Client s3;
    private final S3Presigner presigner;

    S3StorageClient(String endpoint, String accessKey, String secretKey) {
        URI uri = URI.create(endpoint);
        StaticCredentialsProvider creds =
                StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        S3Configuration cfg = S3Configuration.builder().pathStyleAccessEnabled(true).build();
        this.s3 = S3Client.builder()
                .endpointOverride(uri)
                .credentialsProvider(creds)
                .region(Region.US_EAST_1)
                .serviceConfiguration(cfg)
                .httpClient(UrlConnectionHttpClient.create())
                .build();
        this.presigner = S3Presigner.builder()
                .endpointOverride(uri)
                .credentialsProvider(creds)
                .region(Region.US_EAST_1)
                .serviceConfiguration(cfg)
                .build();
    }

    // ---- simple object operations ----

    /** Create the bucket if it does not exist (idempotent). */
    void ensureBucket(String bucket) {
        try {
            s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            return;
        } catch (NoSuchBucketException ignored) {
            // fall through to create
        } catch (S3Exception e) {
            if (e.statusCode() != 404) {
                throw new FileStorageException("Failed to stat bucket: " + bucket, e);
            }
        }
        try {
            s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        } catch (BucketAlreadyOwnedByYouException | BucketAlreadyExistsException ignored) {
            // raced with another creator — fine
        } catch (Exception e) {
            throw new FileStorageException("Failed to create bucket: " + bucket, e);
        }
    }

    /** Upload an object from a stream of known length. */
    void putObject(String bucket, String key, InputStream in, long size, String contentType) {
        try {
            PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(bucket).key(key).contentType(contentType).build();
            RequestBody body = size >= 0
                    ? RequestBody.fromInputStream(in, size)
                    : RequestBody.fromBytes(in.readAllBytes());
            s3.putObject(req, body);
        } catch (Exception e) {
            throw new FileStorageException("Failed to upload object: " + key, e);
        }
    }

    /** Open a stream to read an object; caller closes it. */
    ResponseInputStream<GetObjectResponse> getObject(String bucket, String key) {
        try {
            return s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (Exception e) {
            throw new FileStorageException("Failed to download object: " + key, e);
        }
    }

    /** Delete an object (idempotent). */
    void deleteObject(String bucket, String key) {
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (Exception e) {
            throw new FileStorageException("Failed to delete object: " + key, e);
        }
    }

    /** List up to {@code limit} objects under {@code prefix}. */
    List<FileObject> list(String bucket, String prefix, int limit) {
        int max = limit > 0 ? limit : DEFAULT_LIST_MAX;
        ListObjectsV2Request.Builder req = ListObjectsV2Request.builder().bucket(bucket).maxKeys(max);
        if (prefix != null && !prefix.isBlank()) {
            req.prefix(prefix);
        }
        try {
            ListObjectsV2Response resp = s3.listObjectsV2(req.build());
            List<FileObject> out = new ArrayList<>();
            for (S3Object o : resp.contents()) {
                out.add(new FileObject(
                        o.key(),
                        o.size() == null ? 0 : o.size(),
                        o.lastModified() == null ? null : o.lastModified().toString(),
                        o.eTag()));
                if (limit > 0 && out.size() >= limit) {
                    break;
                }
            }
            return out;
        } catch (Exception e) {
            throw new FileStorageException("Failed to list objects under: " + prefix, e);
        }
    }

    /** Time-limited pre-signed GET URL. */
    String presignGet(String bucket, String key, Duration expiry) {
        try {
            return presigner.presignGetObject(GetObjectPresignRequest.builder()
                    .signatureDuration(expiry)
                    .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
                    .build()).url().toString();
        } catch (Exception e) {
            throw new FileStorageException("Failed to presign GET url: " + key, e);
        }
    }

    // ---- multipart (chunked) upload ----

    /** Initiate an upload; returns the upload id. */
    String createUploadId(String bucket, String object, String contentType) {
        try {
            CreateMultipartUploadRequest.Builder req = CreateMultipartUploadRequest.builder()
                    .bucket(bucket).key(object);
            if (contentType != null && !contentType.isBlank()) {
                req.contentType(contentType);
            }
            return s3.createMultipartUpload(req.build()).uploadId();
        } catch (Exception e) {
            throw new FileStorageException("Failed to init multipart upload for object: " + object, e);
        }
    }

    /** Pre-signed PUT URL for a single 1-based part. */
    String presignPartUrl(String bucket, String object, String uploadId, int partNumber, Duration expiry) {
        try {
            UploadPartRequest part = UploadPartRequest.builder()
                    .bucket(bucket).key(object).uploadId(uploadId).partNumber(partNumber).build();
            return presigner.presignUploadPart(UploadPartPresignRequest.builder()
                    .signatureDuration(expiry)
                    .uploadPartRequest(part)
                    .build()).url().toString();
        } catch (Exception e) {
            throw new FileStorageException("Failed to presign part url for object: " + object, e);
        }
    }

    /**
     * List all uploaded parts (follows pagination), merge them into the object, and
     * return the <b>actual</b> merged size in bytes (summed from the part sizes the
     * store reports — no extra request). Callers can trust this over any
     * client-declared size.
     */
    long complete(String bucket, String object, String uploadId) {
        try {
            List<CompletedPart> completed = new ArrayList<>();
            long totalBytes = 0L;
            Integer marker = null;
            while (true) {
                ListPartsRequest.Builder req = ListPartsRequest.builder()
                        .bucket(bucket).key(object).uploadId(uploadId);
                if (marker != null) {
                    req.partNumberMarker(marker);
                }
                ListPartsResponse resp = s3.listParts(req.build());
                for (Part p : resp.parts()) {
                    completed.add(CompletedPart.builder().partNumber(p.partNumber()).eTag(p.eTag()).build());
                    if (p.size() != null) {
                        totalBytes += p.size();
                    }
                }
                if (Boolean.TRUE.equals(resp.isTruncated())) {
                    marker = resp.nextPartNumberMarker();
                } else {
                    break;
                }
            }
            if (completed.isEmpty()) {
                throw new FileStorageException("No uploaded parts found for uploadId=" + uploadId
                        + " (object=" + object + ")");
            }
            completed.sort(Comparator.comparingInt(CompletedPart::partNumber));
            s3.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                    .bucket(bucket).key(object).uploadId(uploadId)
                    .multipartUpload(CompletedMultipartUpload.builder().parts(completed).build())
                    .build());
            return totalBytes;
        } catch (FileStorageException e) {
            throw e;
        } catch (Exception e) {
            throw new FileStorageException("Failed to complete multipart upload for object: " + object, e);
        }
    }

    /** Abort the upload and discard partial chunks (idempotent). */
    void abort(String bucket, String object, String uploadId) {
        try {
            s3.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                    .bucket(bucket).key(object).uploadId(uploadId).build());
        } catch (Exception e) {
            throw new FileStorageException("Failed to abort multipart upload for object: " + object, e);
        }
    }

    @Override
    public void close() {
        s3.close();
        presigner.close();
    }
}
