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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Central helper for masking sensitive values before they are written to logs,
 * audit records, or any other low-trust sink.
 *
 * <p>Two complementary mechanisms exist in this package:
 * <ul>
 *   <li>{@link Desensitize} — a Jackson field annotation for masking values in
 *       <em>API responses</em> (you know the field at design time);</li>
 *   <li>this util — masks by <em>field name</em> across an arbitrary object graph
 *       (you do <em>not</em> know the shape, e.g. logging controller arguments).</li>
 * </ul>
 *
 * <p>The masking is name-based: any JSON property whose name contains one of the
 * {@linkplain #SENSITIVE_KEYS sensitive fragments} (case-insensitive) has its
 * value replaced with {@link #MASK}, recursively, anywhere in the tree.
 *
 * <p>Thread-safe: the backing {@link ObjectMapper} is configured once and only
 * used for read/write of immutable trees.
 *
 * @author mateaix
 */
public final class SensitiveDataUtil {

    private SensitiveDataUtil() {
    }

    /** The replacement written in place of a sensitive value. */
    public static final String MASK = "******";

    /**
     * Field-name fragments treated as sensitive (matched case-insensitively as a
     * substring, so {@code password} also catches {@code newPassword},
     * {@code oldPassword}, {@code passwordHash}, …).
     */
    public static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "passwd", "pwd",
            "secret", "token", "credential", "credentials",
            "privatekey", "private_key", "accesskey", "access_key",
            "securitycode", "security_code", "cvv", "apikey", "api_key",
            "salt", "signature"
    );

    /** Lenient mapper for log/audit serialization — never throws on odd beans. */
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    /**
     * Serialize {@code value} to JSON with every sensitive field masked.
     * Falls back to a placeholder (never throws) so logging can't break the
     * business call.
     */
    public static String maskToJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            JsonNode root = MAPPER.valueToTree(value);
            maskTree(root);
            return MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            return "[unserializable:" + value.getClass().getSimpleName() + "]";
        }
    }

    /** Whether a property name is considered sensitive. */
    public static boolean isSensitive(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return false;
        }
        String lower = fieldName.toLowerCase(Locale.ROOT);
        for (String key : SENSITIVE_KEYS) {
            if (lower.contains(key)) {
                return true;
            }
        }
        return false;
    }

    /** Recursively mask sensitive fields in-place. */
    private static void maskTree(JsonNode node) {
        if (node instanceof ObjectNode obj) {
            Iterator<Map.Entry<String, JsonNode>> fields = obj.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                if (isSensitive(entry.getKey()) && entry.getValue() != null
                        && !entry.getValue().isNull()) {
                    obj.put(entry.getKey(), MASK);
                } else {
                    maskTree(entry.getValue());
                }
            }
        } else if (node instanceof ArrayNode arr) {
            for (JsonNode child : arr) {
                maskTree(child);
            }
        }
    }
}
