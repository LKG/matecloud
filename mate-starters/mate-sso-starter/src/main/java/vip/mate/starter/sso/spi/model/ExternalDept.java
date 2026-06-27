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
package vip.mate.starter.sso.spi.model;

import java.util.Map;

/**
 * Vendor-neutral department node.
 *
 * @param externalId       provider-side department id (join key)
 * @param name             department name
 * @param parentExternalId parent department's external id ({@code null}/"0" = root)
 * @param sort             ordering hint
 * @param raw              original payload
 *
 * @author mateaix
 */
public record ExternalDept(
        String externalId,
        String name,
        String parentExternalId,
        int sort,
        Map<String, Object> raw) {
}
