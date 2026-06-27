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
package vip.mate.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * MateCloud AI service entry point.
 *
 * <p>Delivers the AI assistant suite (chat workspace, CLI agent management,
 * MCP servers, model/key config, tool registry) on top of Spring AI 2 via
 * the shared {@code mate-ai-starter}.</p>
 *
 * @author mateaix
 */
@SpringBootApplication(scanBasePackages = "vip.mate.ai")
@EnableDiscoveryClient
@EnableAsync
public class MateAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MateAiApplication.class, args);
    }
}
