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
package vip.mate.starter.security.desensitize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.io.IOException;

/**
 * Contextual serializer that applies desensitization based on {@link Desensitize}.
 *
 * @author mateaix
 */
public class DesensitizeSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private DesensitizeType type = DesensitizeType.CUSTOM;
    private int start = 0;
    private int end = 0;

    public DesensitizeSerializer() {
    }

    public DesensitizeSerializer(DesensitizeType type, int start, int end) {
        this.type = type;
        this.start = start;
        this.end = end;
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
            throws JsonMappingException {
        if (property == null) return this;
        Desensitize ann = property.getAnnotation(Desensitize.class);
        if (ann == null) {
            return prov.findValueSerializer(String.class, property);
        }
        return new DesensitizeSerializer(ann.type(), ann.start(), ann.end());
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null || value.isEmpty()) {
            gen.writeString(value);
            return;
        }
        gen.writeString(mask(value));
    }

    private String mask(String value) {
        return switch (type) {
            case MOBILE -> maskMobile(value);
            case EMAIL -> maskEmail(value);
            case ID_CARD -> maskIdCard(value);
            case BANK_CARD -> maskBankCard(value);
            case NAME -> maskName(value);
            case PASSWORD -> "******";
            case ADDRESS -> value.length() > 6 ? value.substring(0, 6) + "***" : "***";
            case CUSTOM -> maskCustom(value);
        };
    }

    private String maskMobile(String v) {
        if (v.length() != 11) return v;
        return v.substring(0, 3) + "****" + v.substring(7);
    }

    private String maskEmail(String v) {
        int at = v.indexOf('@');
        if (at <= 1) return v;
        return v.charAt(0) + "***" + v.substring(at);
    }

    private String maskIdCard(String v) {
        if (v.length() < 10) return v;
        return v.substring(0, 4) + "**********" + v.substring(v.length() - 4);
    }

    private String maskBankCard(String v) {
        if (v.length() < 8) return v;
        return "**** **** **** " + v.substring(v.length() - 4);
    }

    private String maskName(String v) {
        if (v.length() <= 1) return "*";
        return v.charAt(0) + "*".repeat(Math.max(1, v.length() - 1));
    }

    private String maskCustom(String v) {
        int s = Math.max(0, start);
        int e = Math.min(v.length(), end);
        if (s >= e) return v;
        StringBuilder sb = new StringBuilder(v);
        for (int i = s; i < e; i++) sb.setCharAt(i, '*');
        return sb.toString();
    }
}
