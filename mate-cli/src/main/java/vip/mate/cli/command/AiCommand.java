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
package vip.mate.cli.command;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import vip.mate.cli.config.CliConfig;
import vip.mate.cli.http.JsonHttpClient;
import vip.mate.cli.nacos.NacosClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@code mate ai tools|chat|providers}
 * <p>
 * Talks to the AI endpoints (/api/v1/ai/chat, /api/v1/ai/tools) exposed by
 * any service that includes mate-ai-starter. Services are discovered from
 * Nacos when possible; if none is reachable, falls back to the admin base URL
 * (env {@code MATE_ADMIN_URL}).
 */
@Command(name = "ai", description = "AI tool inspection and conversational chat",
        subcommands = {AiCommand.ToolsSub.class, AiCommand.ChatSub.class, AiCommand.ProvidersSub.class})
public class AiCommand implements Runnable {
    @Override
    public void run() {
        System.out.println("Usage: mate ai <tools|chat|providers>");
    }

    /** Aggregates /api/v1/ai/tools across every Nacos-registered service. */
    @Command(name = "tools", description = "List every @Tool exposed by AI-enabled services")
    public static class ToolsSub implements Runnable {
        @Override
        public void run() {
            JsonHttpClient http = new JsonHttpClient();
            List<String> endpoints = discoverAiEndpoints();
            if (endpoints.isEmpty()) {
                System.out.println("(no AI-enabled services found)");
                return;
            }
            System.out.printf("%-30s %-18s %s%n", "TOOL", "SERVICE", "DESCRIPTION");
            System.out.println("-".repeat(90));
            for (String ep : endpoints) {
                try {
                    Map<String, Object> resp = http.get(ep + "/api/v1/ai/tools");
                    Object data = resp.get("data");
                    if (!(data instanceof List<?> list)) continue;
                    String svcLabel = labelFor(ep);
                    for (Object t : list) {
                        if (!(t instanceof Map<?, ?> m)) continue;
                        String name = String.valueOf(m.get("name"));
                        String desc = String.valueOf(m.get("description"));
                        if (desc.length() > 50) desc = desc.substring(0, 50) + "...";
                        System.out.printf("%-30s %-18s %s%n", name, svcLabel, desc);
                    }
                } catch (Exception e) {
                    System.err.println("[warn] " + ep + " — " + e.getMessage());
                }
            }
        }
    }

    /** {@code mate ai chat "What users signed up today?"} */
    @Command(name = "chat", description = "Ask the AI; it will pick + call @Tool methods automatically")
    public static class ChatSub implements Runnable {
        @Parameters(index = "0..*", description = "Your question")
        List<String> message;

        @Option(names = "--service", description = "Target service (default: admin)")
        String service;

        @Option(names = "--conversation", description = "Conversation id for multi-turn memory")
        String conversationId;

        @Override
        public void run() {
            if (message == null || message.isEmpty()) {
                System.err.println("Missing message. Usage: mate ai chat \"your question\"");
                return;
            }
            String endpoint = resolveEndpoint(service);
            String question = String.join(" ", message);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("message", question);
            body.put("conversationId",
                    conversationId == null ? "mate-cli-" + UUID.randomUUID() : conversationId);
            try {
                Map<String, Object> resp = new JsonHttpClient().postJson(endpoint + "/api/v1/ai/chat", body);
                Object data = resp.get("data");
                System.out.println(data == null ? "(empty response)" : data);
            } catch (Exception e) {
                System.err.println("Chat failed: " + e.getMessage());
            }
        }
    }

    @Command(name = "providers", description = "List supported LLM providers")
    public static class ProvidersSub implements Runnable {
        @Override
        public void run() {
            System.out.println("Supported providers (set MATE_AI_PROVIDER or spring.ai.model.chat):");
            System.out.println();
            System.out.println("  anthropic   Claude Opus 4.8 / Sonnet 4.6 / Haiku 4.5      (default)");
            System.out.println("  openai      GPT-5.5 / 5.5-Pro / 5.3-Codex  [also: KIMI/Moonshot via OPENAI_BASE_URL override]");
            System.out.println("  zhipuai     GLM-5 / GLM-4.7");
            System.out.println("  minimax     abab6.5s-chat");
            System.out.println("  deepseek    deepseek-v4 / deepseek-v4-flash / deepseek-r1");
            System.out.println("  ollama      Self-hosted (qwen2.5, llama3.x, etc.)");
            System.out.println();
            System.out.println("Each provider's api-key comes from the matching env var:");
            System.out.println("  ANTHROPIC_API_KEY / OPENAI_API_KEY / ZHIPUAI_API_KEY / MINIMAX_API_KEY /");
            System.out.println("  DEEPSEEK_API_KEY");
        }
    }

    // ---- Shared helpers ----

    static List<String> discoverAiEndpoints() {
        List<String> endpoints = new ArrayList<>();
        try {
            NacosClient nacos = new NacosClient();
            for (String svc : nacos.listServices()) {
                for (Map<String, Object> inst : nacos.listInstances(svc)) {
                    endpoints.add("http://" + inst.get("ip") + ":" + inst.get("port"));
                }
            }
        } catch (Exception ignored) {
            // Fall back to the admin URL
        }
        if (endpoints.isEmpty()) {
            endpoints.add(CliConfig.adminBaseUrl());
        }
        return endpoints;
    }

    static String resolveEndpoint(String serviceName) {
        if (serviceName == null || serviceName.isBlank()) {
            return CliConfig.adminBaseUrl();
        }
        try {
            NacosClient nacos = new NacosClient();
            List<Map<String, Object>> instances = nacos.listInstances(serviceName);
            if (!instances.isEmpty()) {
                Map<String, Object> inst = instances.get(0);
                return "http://" + inst.get("ip") + ":" + inst.get("port");
            }
        } catch (Exception ignored) {
        }
        return CliConfig.adminBaseUrl();
    }

    static String labelFor(String endpoint) {
        int colon = endpoint.lastIndexOf(':');
        return colon > 0 ? endpoint.substring(colon + 1) : endpoint;
    }
}
