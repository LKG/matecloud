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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Self-description of a channel provider (storage backend, SMS gateway, …):
 * its type key, display name, and the config fields the admin UI must collect.
 *
 * <p>Providers return this from their SPI {@code descriptor()} method so the
 * platform can render a generic config form and validate input — replacing the
 * "one hand-written form per provider" approach.
 *
 * @author mateaix
 */
public final class ProviderDescriptor {

    private final String type;
    private final String name;
    private final String describe;
    private final List<FieldSpec> fields;

    private ProviderDescriptor(String type, String name, String describe, List<FieldSpec> fields) {
        this.type = type;
        this.name = name;
        this.describe = describe;
        this.fields = Collections.unmodifiableList(fields);
    }

    public String getType() { return type; }
    public String getName() { return name; }
    public String getDescribe() { return describe; }
    public List<FieldSpec> getFields() { return fields; }

    public static Builder builder(String type, String name) {
        return new Builder(type, name);
    }

    public static final class Builder {
        private final String type;
        private final String name;
        private String describe = "";
        private final List<FieldSpec> fields = new ArrayList<>();

        private Builder(String type, String name) {
            this.type = type;
            this.name = name;
        }

        public Builder describe(String describe) {
            this.describe = describe;
            return this;
        }

        public Builder text(String key, String label, boolean required) {
            return field(new FieldSpec(key, label, FieldType.TEXT, required, null, null));
        }

        public Builder text(String key, String label, boolean required, String placeholder) {
            return field(new FieldSpec(key, label, FieldType.TEXT, required, placeholder, null));
        }

        public Builder secret(String key, String label, boolean required) {
            return field(new FieldSpec(key, label, FieldType.SECRET, required, null, null));
        }

        public Builder map(String key, String label, String tip) {
            return field(new FieldSpec(key, label, FieldType.MAP, false, null, tip));
        }

        public Builder field(FieldSpec spec) {
            fields.add(spec);
            return this;
        }

        public ProviderDescriptor build() {
            return new ProviderDescriptor(type, name, describe, fields);
        }
    }
}
