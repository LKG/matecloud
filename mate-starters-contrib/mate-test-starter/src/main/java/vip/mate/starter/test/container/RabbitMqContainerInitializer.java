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
package vip.mate.starter.test.container;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.lang.NonNull;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared RabbitMQ testcontainer.
 *
 * @author mateaix
 */
public class RabbitMqContainerInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final RabbitMQContainer RABBIT;

    static {
        RABBIT = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management"))
                .withUser("test", "test")
                .withReuse(true);
        RABBIT.start();
    }

    @Override
    public void initialize(@NonNull ConfigurableApplicationContext ctx) {
        TestPropertyValues.of(
                "spring.rabbitmq.host=" + RABBIT.getHost(),
                "spring.rabbitmq.port=" + RABBIT.getAmqpPort(),
                "spring.rabbitmq.username=test",
                "spring.rabbitmq.password=test"
        ).applyTo(ctx.getEnvironment());
    }

    public static RabbitMQContainer getContainer() {
        return RABBIT;
    }
}
