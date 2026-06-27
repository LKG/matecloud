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

/**
 * Resolves which provider type is active for the current call. The default impl
 * returns the static {@code mate.file.type}. A multi-tenant deployment can
 * provide its own bean to choose the provider per tenant/site at runtime — this
 * is the seam that makes per-tenant storage possible without touching the core.
 *
 * @author mateaix
 */
public interface FileStorageResolver {

    String resolveType();
}
