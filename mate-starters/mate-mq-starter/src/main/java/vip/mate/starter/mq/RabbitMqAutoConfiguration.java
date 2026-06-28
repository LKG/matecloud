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
package vip.mate.starter.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.amqp.autoconfigure.RabbitTemplateCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for RabbitMQ event publishing.
 * <p>
 * The broker is only used for cross-service domain-event fan-out, which only
 * exists in microservice mode. In monolith mode ({@code mate.rpc.mode=local})
 * domain events are dispatched in-process via Spring's
 * {@code ApplicationEventPublisher}, so this is disabled and no broker is needed
 * (mirrors {@code RpcAutoConfiguration}).
 *
 * @author mateaix
 */
@AutoConfiguration
@ConditionalOnClass(RabbitTemplate.class)
@ConditionalOnProperty(name = "mate.rpc.mode", havingValue = "dubbo", matchIfMissing = true)
public class RabbitMqAutoConfiguration {

    @Bean
    public MessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * 开启生产侧观测: 发消息时把当前 trace 上下文注入消息头, 与消费侧
     * ({@code DomainEventListenerRegistrar} 容器的 observationEnabled)合起来,
     * traceId 便能跨 MQ 续传。作用于 Boot 自动装配的 {@link RabbitTemplate}。
     */
    @Bean
    public RabbitTemplateCustomizer mateRabbitTemplateObservationCustomizer() {
        return template -> template.setObservationEnabled(true);
    }

    @Bean
    public EventPublisher eventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        return new EventPublisher(rabbitTemplate, objectMapper);
    }
}
