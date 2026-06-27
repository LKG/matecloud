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
package vip.mate.starter.file.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import vip.mate.starter.file.core.FileStorageRegistry;
import vip.mate.starter.file.core.FileStorageResolver;
import vip.mate.starter.file.spi.FileStorage;

/**
 * Marks a {@link FileStorage} implementation and declares the provider type it
 * serves (e.g. {@code "minio"}, {@code "oss"}, {@code "s3"}).
 *
 * <p>The {@link FileStorageRegistry} keys providers by this value, and
 * {@code mate.file.type} (resolved via {@link FileStorageResolver}) selects which
 * one is active. Adding a new cloud is therefore just: implement
 * {@link FileStorage}, annotate it with {@code @FileProvider("xxx")}, register it
 * as a bean — no central switch to edit.
 *
 * @author mateaix
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface FileProvider {

    /** Provider type key, e.g. {@code "minio"}. */
    String value();

    /** Human-readable description (optional). */
    String describe() default "";
}
