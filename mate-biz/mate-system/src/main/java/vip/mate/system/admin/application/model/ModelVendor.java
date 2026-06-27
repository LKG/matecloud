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

/**
 * Supported AI model vendors. The {@code vendor} key persisted in
 * {@code mate_model_provider.vendor} matches one of these names.
 *
 * @author mateaix
 */
public enum ModelVendor {
    OPENAI,
    AZURE,
    ANTHROPIC,
    GEMINI,
    GROK,
    MISTRAL,
    GROQ,
    OPENROUTER,
    DEEPSEEK,
    QWEN,
    ZHIPU,
    MOONSHOT,
    MINIMAX,
    BAICHUAN,
    WENXIN,
    SPARK,
    HUNYUAN,
    STEPFUN,
    SILICONFLOW,
    OLLAMA,
    XINFERENCE,
    OPENLLM,
    JINA,
    COHERE,
    AZURE_TTS,
    COSYVOICE,
    FLUX,
    KLING,
    LUMA,
    GATEWAY,
    CUSTOM
}
