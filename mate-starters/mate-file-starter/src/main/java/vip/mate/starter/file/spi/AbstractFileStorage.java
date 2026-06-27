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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import vip.mate.base.channel.ChannelConfigStore;
import vip.mate.starter.file.annotation.FileProvider;
import vip.mate.starter.file.model.UploadCommand;

/**
 * Base for {@link FileStorage} implementations. Provides the provider-type
 * lookup (from {@link FileProvider}), a default object-key generator, and
 * content-type sanitisation shared by all providers.
 *
 * @author mateaix
 */
public abstract class AbstractFileStorage implements FileStorage {

    /** Logical channel key used when consulting the {@link ChannelConfigStore}. */
    public static final String CHANNEL = "storage";

    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /**
     * Content types a browser renders inline — forced to octet-stream so an
     * uploaded {@code .html}/{@code .svg} served from a web-exposed bucket can't
     * execute as stored XSS.
     */
    private static final Set<String> RENDERABLE_BLOCKLIST = Set.of(
            "text/html", "application/xhtml+xml", "image/svg+xml",
            "application/xml", "text/xml", "application/javascript", "text/javascript");

    @Override
    public String type() {
        FileProvider provider = getClass().getAnnotation(FileProvider.class);
        return provider != null ? provider.value() : "unknown";
    }

    /** Use the caller-supplied object name, else generate {@code yyyy/MM/dd/uuid.ext}. */
    protected String resolveObjectName(UploadCommand command) {
        if (command.getObjectName() != null && !command.getObjectName().isBlank()) {
            return command.getObjectName();
        }
        return generateObjectName(command.getOriginalFilename());
    }

    /** Generate a {@code yyyy/MM/dd/uuid.ext} object key (ext taken from {@code originalFilename}). */
    protected String generateObjectName(String originalFilename) {
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        return LocalDate.now().format(DATE_PATH) + "/"
                + UUID.randomUUID().toString().replace("-", "") + ext;
    }

    protected String sanitizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "application/octet-stream";
        }
        String base = contentType.toLowerCase(Locale.ROOT).split(";")[0].trim();
        return RENDERABLE_BLOCKLIST.contains(base) ? "application/octet-stream" : contentType;
    }

    /** Resolve a possibly-blank bucket argument to the provider default. */
    protected String bucketOrDefault(String bucket) {
        return (bucket == null || bucket.isBlank()) ? defaultBucket() : bucket;
    }

    /**
     * Merge admin-managed config (from the {@link ChannelConfigStore}, if any) over
     * the provider's static defaults. Non-blank stored values win, so the admin
     * console can override {@code *Properties} at runtime (and per tenant).
     */
    protected Map<String, String> effectiveConfig(ChannelConfigStore store, Map<String, String> staticDefaults) {
        Map<String, String> merged = new LinkedHashMap<>(staticDefaults);
        if (store != null) {
            Map<String, String> stored = store.config(CHANNEL, type());
            if (stored != null) {
                stored.forEach((k, v) -> {
                    if (v != null && !v.isBlank()) {
                        merged.put(k, v);
                    }
                });
            }
        }
        return merged;
    }
}

