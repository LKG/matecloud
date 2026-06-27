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

import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Carrier for an upload request. Built via chained setters so adding options
 * later never breaks call sites. Supply EITHER a {@link MultipartFile} (the
 * common web path) OR a raw {@code (inputStream, size, contentType)} triple.
 *
 * <p>{@code bucket}/{@code objectName} are optional — when blank the provider
 * uses its default bucket and generates a {@code yyyy/MM/dd/uuid.ext} key.
 *
 * @author mateaix
 */
public class UploadCommand {

    private MultipartFile multipartFile;
    private InputStream inputStream;
    private long size = -1;
    private String contentType;
    private String originalFilename;
    private String bucket;
    private String objectName;
    private final Map<String, Object> extendData = new HashMap<>();

    public static UploadCommand of(MultipartFile file) {
        UploadCommand c = new UploadCommand();
        c.multipartFile = file;
        c.originalFilename = file == null ? null : file.getOriginalFilename();
        return c;
    }

    public static UploadCommand of(InputStream inputStream, long size, String contentType, String objectName) {
        UploadCommand c = new UploadCommand();
        c.inputStream = inputStream;
        c.size = size;
        c.contentType = contentType;
        c.objectName = objectName;
        return c;
    }

    public UploadCommand bucket(String bucket) { this.bucket = bucket; return this; }
    public UploadCommand objectName(String objectName) { this.objectName = objectName; return this; }
    public UploadCommand originalFilename(String originalFilename) { this.originalFilename = originalFilename; return this; }
    public UploadCommand contentType(String contentType) { this.contentType = contentType; return this; }
    public UploadCommand addExtend(String key, Object value) { this.extendData.put(key, value); return this; }

    public MultipartFile getMultipartFile() { return multipartFile; }
    public InputStream getInputStream() { return inputStream; }
    public long getSize() { return size; }
    public String getContentType() { return contentType; }
    public String getOriginalFilename() { return originalFilename; }
    public String getBucket() { return bucket; }
    public String getObjectName() { return objectName; }
    public Map<String, Object> getExtendData() { return extendData; }
    @SuppressWarnings("unchecked")
    public <T> T getExtend(String key) { return (T) extendData.get(key); }
}
