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
package vip.mate.base.channel;

/**
 * Metadata for one config field of a channel provider. The admin UI renders a
 * generic dynamic form from a list of these — no per-provider frontend code.
 *
 * @param key         config key (also the JSON key persisted)
 * @param label       display label
 * @param type        input type {@link FieldType}
 * @param required    whether the field is mandatory
 * @param placeholder input placeholder hint
 * @param tip         help text shown under the field
 *
 * @author mateaix
 */
public record FieldSpec(
        String key,
        String label,
        FieldType type,
        boolean required,
        String placeholder,
        String tip) {

    public boolean secret() {
        return type == FieldType.SECRET;
    }
}
