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
package vip.mate.system.admin.application.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Curated catalog of common model ids per {@link ModelVendor}. Used to pre-fill and
 * suggest options in the admin "supported models" picker — operators stay free to add
 * any new id (e.g. {@code deepseek-v4-pro}) that isn't listed here.
 *
 * <p>This is a convenience list only, not an allow-list: vendors without an entry simply
 * return an empty suggestion set.
 *
 * @author mateaix
 */
public final class ModelCatalog {

    private static final Map<ModelVendor, List<String>> CATALOG = new LinkedHashMap<>();

    private ModelCatalog() {
    }

    static {
        CATALOG.put(ModelVendor.OPENAI, List.of(
                "gpt-4o", "gpt-4o-mini", "gpt-4.1", "gpt-4.1-mini", "o3", "o4-mini",
                "text-embedding-3-large", "text-embedding-3-small"));
        CATALOG.put(ModelVendor.ANTHROPIC, List.of(
                "claude-opus-4-20250514", "claude-sonnet-4-20250514", "claude-3-7-sonnet-latest",
                "claude-3-5-sonnet-latest", "claude-3-5-haiku-latest"));
        CATALOG.put(ModelVendor.GEMINI, List.of(
                "gemini-2.5-pro", "gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-pro",
                "text-embedding-004"));
        CATALOG.put(ModelVendor.DEEPSEEK, List.of(
                "deepseek-chat", "deepseek-reasoner"));
        CATALOG.put(ModelVendor.QWEN, List.of(
                "qwen-max", "qwen-plus", "qwen-turbo", "qwen2.5-72b-instruct", "text-embedding-v3"));
        CATALOG.put(ModelVendor.ZHIPU, List.of(
                "glm-4-plus", "glm-4-air", "glm-4-flash", "glm-4v-plus", "embedding-3"));
        CATALOG.put(ModelVendor.MOONSHOT, List.of(
                "kimi-latest", "moonshot-v1-8k", "moonshot-v1-32k", "moonshot-v1-128k"));
        CATALOG.put(ModelVendor.GROK, List.of(
                "grok-3", "grok-3-mini", "grok-2-vision-latest"));
        CATALOG.put(ModelVendor.MISTRAL, List.of(
                "mistral-large-latest", "mistral-small-latest", "codestral-latest", "mistral-embed"));
        CATALOG.put(ModelVendor.MINIMAX, List.of(
                "MiniMax-Text-01", "abab6.5s-chat"));
        CATALOG.put(ModelVendor.JINA, List.of(
                "jina-embeddings-v3", "jina-reranker-v2-base-multilingual"));
        CATALOG.put(ModelVendor.COHERE, List.of(
                "embed-multilingual-v3.0", "rerank-multilingual-v3.0"));
        CATALOG.put(ModelVendor.SILICONFLOW, List.of(
                "Qwen/Qwen2.5-72B-Instruct", "BAAI/bge-m3"));
    }

    /** Suggested model ids for a vendor, or an empty list if none are curated. */
    public static List<String> suggested(ModelVendor vendor) {
        return vendor == null ? List.of() : CATALOG.getOrDefault(vendor, List.of());
    }

    /** Suggested model ids for a vendor name (case-insensitive), empty if unknown. */
    public static List<String> suggested(String vendor) {
        if (vendor == null) {
            return List.of();
        }
        try {
            return suggested(ModelVendor.valueOf(vendor.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return List.of();
        }
    }
}
