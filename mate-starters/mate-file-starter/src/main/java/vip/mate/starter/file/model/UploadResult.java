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
package vip.mate.starter.file.model;

/**
 * Result of a successful upload.
 *
 * @param objectName       storage key (relative path within the bucket)
 * @param bucket           bucket the object landed in
 * @param url              public access URL (may require the bucket to be public)
 * @param size             stored size in bytes ({@code -1} if unknown)
 * @param originalFilename original client filename, if any
 * @param providerType     the provider type that handled it (e.g. {@code "minio"})
 *
 * @author mateaix
 */
public record UploadResult(
        String objectName,
        String bucket,
        String url,
        long size,
        String originalFilename,
        String providerType) {
}
