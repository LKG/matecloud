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
package vip.mate.starter.mq.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.util.Map;

/**
 * Auto-configuration for the domain event handling infrastructure.
 * <p>
 * Declares the shared topic exchange ({@code mate.domain.events}), a dead-letter
 * exchange + queue, and registers the {@link DomainEventListenerRegistrar}
 * {@link org.springframework.beans.factory.config.BeanPostProcessor}.
 * <p>
 * This is the cross-service ({@code @DomainEventHandler}) broker fan-out and only
 * applies in microservice mode. In monolith mode ({@code mate.rpc.mode=local})
 * domain events stay in-process via Spring's {@code ApplicationEventPublisher} +
 * {@code @TransactionalEventListener}, so this — and its eager
 * {@code BeanPostProcessor} — is switched off entirely (no broker required).
 *
 * @author mateaix
 */
@AutoConfiguration
@ConditionalOnClass(ConnectionFactory.class)
@ConditionalOnProperty(name = "mate.rpc.mode", havingValue = "dubbo", matchIfMissing = true)
public class DomainEventAutoConfiguration {

    /**
     * Primary topic exchange for all domain events.
     */
    @Bean
    public TopicExchange domainEventsExchange() {
        return new TopicExchange("mate.domain.events", true, false);
    }

    /**
     * Dead-letter exchange for failed domain events.
     */
    @Bean
    public TopicExchange domainEventsDlxExchange() {
        return new TopicExchange("mate.domain.events.dlx", true, false);
    }

    /**
     * Dead-letter queue with a 7-day TTL (604 800 000 ms).
     */
    @Bean
    public Queue domainEventsDlq() {
        Map<String, Object> args = Map.of(
                "x-message-ttl", 604_800_000
        );
        return new Queue("mate.domain.events.dlq", true, false, false, args);
    }

    /**
     * Bind the DLQ to the DLX with a catch-all routing key.
     */
    @Bean
    public Binding domainEventsDlqBinding(Queue domainEventsDlq, TopicExchange domainEventsDlxExchange) {
        return BindingBuilder.bind(domainEventsDlq).to(domainEventsDlxExchange).with("#");
    }

    @Bean
    public RabbitAdmin domainEventsRabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public DomainEventListenerRegistrar domainEventListenerRegistrar(RabbitAdmin domainEventsRabbitAdmin,
                                                                     ConnectionFactory connectionFactory,
                                                                     RabbitTemplate rabbitTemplate,
                                                                     ObjectMapper objectMapper,
                                                                     TopicExchange domainEventsExchange) {
        return new DomainEventListenerRegistrar(
                domainEventsRabbitAdmin,
                connectionFactory,
                rabbitTemplate,
                objectMapper,
                domainEventsExchange);
    }
}
