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

import vip.mate.base.channel.ProviderDescriptor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Static registry of AI model vendors. For each {@link ModelVendor} it pairs the
 * credential {@link ProviderDescriptor} (the config fields the admin UI must collect)
 * with the list of {@link ModelType modalities} that vendor can serve.
 *
 * <p>Mirrors the channel-config approach: a self-describing descriptor drives a generic
 * dynamic form, so adding a vendor is a one-line registration here, not new UI code.
 *
 * @author mateaix
 */
public final class ModelProviderDescriptors {

    /** A vendor's descriptor plus the model types it can serve. */
    public record VendorSpec(ModelVendor vendor, ProviderDescriptor descriptor, List<ModelType> modalities) {}

    private static final Map<ModelVendor, VendorSpec> REGISTRY = new LinkedHashMap<>();

    private ModelProviderDescriptors() {
    }

    static {
        register(ModelVendor.OPENAI, ProviderDescriptor.builder("OPENAI", "OpenAI")
                        .describe("OpenAI 兼容接口(GPT / text-embedding / TTS / Whisper)")
                        .secret("apiKey", "API Key", true)
                        .text("baseUrl", "Base URL", false, "https://api.openai.com")
                        .build(),
                List.of(ModelType.LLM, ModelType.EMBEDDING, ModelType.TTS, ModelType.STT,
                        ModelType.IMAGE, ModelType.MODERATION));

        register(ModelVendor.AZURE, ProviderDescriptor.builder("AZURE", "Azure OpenAI")
                        .describe("Azure 托管的 OpenAI 模型")
                        .secret("apiKey", "API Key", true)
                        .text("endpoint", "Endpoint", true, "https://{resource}.openai.azure.com")
                        .text("deployment", "Deployment", true)
                        .text("apiVersion", "API Version", false, "2024-02-15-preview")
                        .build(),
                List.of(ModelType.LLM, ModelType.EMBEDDING));

        register(ModelVendor.DEEPSEEK, ProviderDescriptor.builder("DEEPSEEK", "DeepSeek")
                        .describe("DeepSeek 开放平台(OpenAI 兼容)")
                        .secret("apiKey", "API Key", true)
                        .text("baseUrl", "Base URL", false, "https://api.deepseek.com")
                        .build(),
                List.of(ModelType.LLM));

        register(ModelVendor.QWEN, ProviderDescriptor.builder("QWEN", "通义千问 Qwen")
                        .describe("阿里云百炼 DashScope(OpenAI 兼容)")
                        .secret("apiKey", "API Key", true)
                        .text("baseUrl", "Base URL", false,
                                "https://dashscope.aliyuncs.com/compatible-mode/v1")
                        .build(),
                List.of(ModelType.LLM, ModelType.EMBEDDING, ModelType.RERANK));

        register(ModelVendor.OLLAMA, ProviderDescriptor.builder("OLLAMA", "Ollama")
                        .describe("自托管 Ollama(OpenAI 兼容)")
                        .text("baseUrl", "Base URL", true, "http://localhost:11434")
                        .build(),
                List.of(ModelType.LLM, ModelType.EMBEDDING));

        register(ModelVendor.JINA, ProviderDescriptor.builder("JINA", "Jina AI")
                        .describe("Jina embeddings / reranker")
                        .secret("apiKey", "API Key", true)
                        .text("baseUrl", "Base URL", false, "https://api.jina.ai")
                        .build(),
                List.of(ModelType.EMBEDDING, ModelType.RERANK));

        register(ModelVendor.COHERE, ProviderDescriptor.builder("COHERE", "Cohere")
                        .describe("Cohere embeddings / rerank")
                        .secret("apiKey", "API Key", true)
                        .text("baseUrl", "Base URL", false, "https://api.cohere.com")
                        .build(),
                List.of(ModelType.EMBEDDING, ModelType.RERANK));

        register(ModelVendor.AZURE_TTS, ProviderDescriptor.builder("AZURE_TTS", "Azure 语音")
                        .describe("Azure Cognitive Services 语音(TTS / STT)")
                        .secret("apiKey", "API Key", true)
                        .text("region", "Region", true, "eastus")
                        .build(),
                List.of(ModelType.TTS, ModelType.STT));

        register(ModelVendor.COSYVOICE, ProviderDescriptor.builder("COSYVOICE", "CosyVoice")
                        .describe("阿里 CosyVoice 语音合成")
                        .secret("apiKey", "API Key", true)
                        .text("baseUrl", "Base URL", false,
                                "https://dashscope.aliyuncs.com")
                        .build(),
                List.of(ModelType.TTS));

        register(ModelVendor.FLUX, ProviderDescriptor.builder("FLUX", "FLUX")
                        .describe("Black Forest Labs FLUX 文生图")
                        .secret("apiKey", "API Key", true)
                        .text("baseUrl", "Base URL", false, "https://api.bfl.ai")
                        .build(),
                List.of(ModelType.IMAGE));

        register(ModelVendor.KLING, ProviderDescriptor.builder("KLING", "可灵 Kling")
                        .describe("快手可灵 文生视频")
                        .secret("accessKey", "Access Key", true)
                        .secret("secretKey", "Secret Key", true)
                        .text("baseUrl", "Base URL", false, "https://api.klingai.com")
                        .build(),
                List.of(ModelType.VIDEO));

        register(ModelVendor.LUMA, ProviderDescriptor.builder("LUMA", "Luma")
                        .describe("Luma Dream Machine 文生视频")
                        .secret("apiKey", "API Key", true)
                        .text("baseUrl", "Base URL", false, "https://api.lumalabs.ai")
                        .build(),
                List.of(ModelType.VIDEO));

        // ---- Additional mainstream vendors (most are OpenAI-compatible) ----
        register(ModelVendor.ANTHROPIC, ProviderDescriptor.builder("ANTHROPIC", "Anthropic Claude")
                        .secret("apiKey", "API Key", true)
                        .text("baseUrl", "Base URL", false, "https://api.anthropic.com")
                        .build(),
                List.of(ModelType.LLM));

        register(ModelVendor.GEMINI, ProviderDescriptor.builder("GEMINI", "Google Gemini")
                        .secret("apiKey", "API Key", true)
                        .text("baseUrl", "Base URL", false, "https://generativelanguage.googleapis.com")
                        .build(),
                List.of(ModelType.LLM, ModelType.EMBEDDING));

        register(ModelVendor.GROK, ProviderDescriptor.builder("GROK", "xAI Grok")
                        .secret("apiKey", "API Key", true)
                        .text("baseUrl", "Base URL", false, "https://api.x.ai/v1")
                        .build(),
                List.of(ModelType.LLM));

        register(ModelVendor.MISTRAL, ProviderDescriptor.builder("MISTRAL", "Mistral AI")
                        .secret("apiKey", "API Key", true)
                        .text("baseUrl", "Base URL", false, "https://api.mistral.ai/v1")
                        .build(),
                List.of(ModelType.LLM, ModelType.EMBEDDING));

        register(ModelVendor.GROQ, ProviderDescriptor.builder("GROQ", "Groq")
                        .secret("apiKey", "API Key", true)
                        .text("baseUrl", "Base URL", false, "https://api.groq.com/openai/v1")
                        .build(),
                List.of(ModelType.LLM));

        register(ModelVendor.OPENROUTER, ProviderDescriptor.builder("OPENROUTER", "OpenRouter")
                        .secret("apiKey", "API Key", true)
                        .text("baseUrl", "Base URL", false, "https://openrouter.ai/api/v1")
                        .build(),
                List.of(ModelType.LLM));

        register(ModelVendor.ZHIPU, ProviderDescriptor.builder("ZHIPU", "智谱 GLM")
                        .secret("apiKey", "API Key", true)
                        .text("baseUrl", "Base URL", false, "https://open.bigmodel.cn/api/paas/v4")
                        .build(),
                List.of(ModelType.LLM, ModelType.EMBEDDING));

        register(ModelVendor.MOONSHOT, ProviderDescriptor.builder("MOONSHOT", "月之暗面 Moonshot")
                        .secret("apiKey", "API Key", true)
                        .text("baseUrl", "Base URL", false, "https://api.moonshot.cn/v1")
                        .build(),
                List.of(ModelType.LLM));

        register(ModelVendor.MINIMAX, ProviderDescriptor.builder("MINIMAX", "MiniMax")
                        .secret("apiKey", "API Key", true)
                        .text("baseUrl", "Base URL", false, "https://api.minimax.chat/v1")
                        .build(),
                List.of(ModelType.LLM, ModelType.TTS));

        register(ModelVendor.BAICHUAN, ProviderDescriptor.builder("BAICHUAN", "百川 Baichuan")
                        .secret("apiKey", "API Key", true)
                        .text("baseUrl", "Base URL", false, "https://api.baichuan-ai.com/v1")
                        .build(),
                List.of(ModelType.LLM, ModelType.EMBEDDING));

        register(ModelVendor.WENXIN, ProviderDescriptor.builder("WENXIN", "百度文心一言")
                        .secret("apiKey", "API Key", true)
                        .text("baseUrl", "Base URL", false, "https://qianfan.baidubce.com/v2")
                        .build(),
                List.of(ModelType.LLM, ModelType.EMBEDDING));

        register(ModelVendor.SPARK, ProviderDescriptor.builder("SPARK", "讯飞星火")
                        .secret("apiKey", "API Key", true)
                        .text("baseUrl", "Base URL", false, "https://spark-api-open.xf-yun.com/v1")
                        .build(),
                List.of(ModelType.LLM));

        register(ModelVendor.HUNYUAN, ProviderDescriptor.builder("HUNYUAN", "腾讯混元")
                        .secret("apiKey", "API Key", true)
                        .text("baseUrl", "Base URL", false, "https://api.hunyuan.cloud.tencent.com/v1")
                        .build(),
                List.of(ModelType.LLM));

        register(ModelVendor.STEPFUN, ProviderDescriptor.builder("STEPFUN", "阶跃星辰 StepFun")
                        .secret("apiKey", "API Key", true)
                        .text("baseUrl", "Base URL", false, "https://api.stepfun.com/v1")
                        .build(),
                List.of(ModelType.LLM));

        register(ModelVendor.SILICONFLOW, ProviderDescriptor.builder("SILICONFLOW", "硅基流动 SiliconFlow")
                        .secret("apiKey", "API Key", true)
                        .text("baseUrl", "Base URL", false, "https://api.siliconflow.cn/v1")
                        .build(),
                List.of(ModelType.LLM, ModelType.EMBEDDING, ModelType.RERANK, ModelType.TTS));

        register(ModelVendor.XINFERENCE, ProviderDescriptor.builder("XINFERENCE", "Xorbits Inference")
                        .text("baseUrl", "Base URL", true, "http://localhost:9997/v1")
                        .build(),
                List.of(ModelType.LLM, ModelType.EMBEDDING, ModelType.RERANK));

        register(ModelVendor.OPENLLM, ProviderDescriptor.builder("OPENLLM", "OpenLLM")
                        .text("baseUrl", "Base URL", true, "http://localhost:3000/v1")
                        .build(),
                List.of(ModelType.LLM, ModelType.EMBEDDING));

        register(ModelVendor.GATEWAY, ProviderDescriptor.builder("GATEWAY", "AI 网关")
                        .describe("经 OpenAI 兼容网关(New-API / LiteLLM / One-API)统一供给")
                        .secret("token", "Gateway Token", true)
                        .text("baseUrl", "Base URL", true, "https://gateway.example.com/v1")
                        .build(),
                List.of(ModelType.LLM, ModelType.EMBEDDING, ModelType.RERANK, ModelType.TTS,
                        ModelType.STT, ModelType.IMAGE, ModelType.VIDEO, ModelType.MODERATION));

        register(ModelVendor.CUSTOM, ProviderDescriptor.builder("CUSTOM", "自定义(OpenAI 兼容)")
                        .describe("任意 OpenAI 兼容端点")
                        .secret("apiKey", "API Key", false)
                        .text("baseUrl", "Base URL", true)
                        .build(),
                List.of(ModelType.LLM, ModelType.EMBEDDING, ModelType.RERANK, ModelType.TTS,
                        ModelType.STT, ModelType.IMAGE, ModelType.VIDEO, ModelType.MODERATION));
    }

    private static void register(ModelVendor vendor, ProviderDescriptor descriptor, List<ModelType> modalities) {
        REGISTRY.put(vendor, new VendorSpec(vendor, descriptor, modalities));
    }

    /** All registered vendor specs, in registration order. */
    public static List<VendorSpec> all() {
        return List.copyOf(REGISTRY.values());
    }

    /** Spec for a vendor, or {@code null} if unknown. */
    public static VendorSpec get(ModelVendor vendor) {
        return REGISTRY.get(vendor);
    }

    /** Spec for a vendor name (case-insensitive), or {@code null} if unknown. */
    public static VendorSpec get(String vendor) {
        if (vendor == null) {
            return null;
        }
        try {
            return REGISTRY.get(ModelVendor.valueOf(vendor.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
