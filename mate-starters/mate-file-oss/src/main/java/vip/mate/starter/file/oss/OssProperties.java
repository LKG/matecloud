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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Aliyun OSS provider properties ({@code mate.file.oss.*}).
 *
 * @author mateaix
 */
@ConfigurationProperties(prefix = "mate.file.oss")
public class OssProperties {

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName = "matecloud";

    /** Optional public access domain (CDN / custom domain). When set, public URLs use it. */
    private String domain;

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getAccessKey() { return accessKey; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    public String getBucketName() { return bucketName; }
    public void setBucketName(String bucketName) { this.bucketName = bucketName; }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
}
