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
package vip.mate.base.model;

/**
 * Resolved, ready-to-call specification of a single AI model endpoint.
 *
 * <p>Produced by {@link ModelConfigStore#resolve(String, String)} from the
 * admin-managed configuration ({@code mate_model_provider} / {@code mate_model_default}
 * / {@code mate_model_gateway}). The {@code apiKey} is already decrypted and is meant
 * to be consumed in-process by the execution layer (mate-model-starter) — it must
 * never be serialized back to a client or written to a log.
 *
 * @param supplyMode {@code LOCAL} (direct vendor call) or {@code GATEWAY} (routed
 *                   through an OpenAI-compatible gateway such as New-API / LiteLLM)
 * @param vendor     vendor key, e.g. {@code OPENAI} / {@code DEEPSEEK} / {@code QWEN}
 * @param baseUrl    OpenAI-compatible base URL to call
 * @param apiKey     decrypted credential (sensitive — keep in-process only)
 * @param model      concrete model name passed to the provider, e.g. {@code gpt-4o}
 *
 * @author mateaix
 */
public record ModelEndpoint(
        String supplyMode,
        String vendor,
        String baseUrl,
        String apiKey,
        String model) {

    /** Supply mode: call the vendor directly with its own credentials. */
    public static final String SUPPLY_LOCAL = "LOCAL";

    /** Supply mode: route through an OpenAI-compatible gateway. */
    public static final String SUPPLY_GATEWAY = "GATEWAY";
}
