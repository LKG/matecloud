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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.charset.StandardCharsets;

/**
 * Event publisher that wraps {@link RabbitTemplate}.
 *
 * @author mateaix
 */
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public EventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(String topic, EventMessage<?> message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            MessageProperties props = new MessageProperties();
            props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            props.setContentEncoding(StandardCharsets.UTF_8.name());
            props.setMessageId(message.getId());

            Message amqpMessage = new Message(json.getBytes(StandardCharsets.UTF_8), props);
            rabbitTemplate.send(topic, "", amqpMessage);

            log.info("Published event to topic [{}], messageId={}", topic, message.getId());
        } catch (Exception e) {
            log.error("Failed to publish event to topic [{}], messageId={}",
                    topic, message.getId(), e);
            throw new RuntimeException("Failed to publish event message", e);
        }
    }

    public void publish(BaseEvent<?> event) {
        publish(event.topic(), event.buildEventMessage());
    }

    public <T> void publish(String topic, T data) {
        publish(topic, EventMessage.of(data));
    }

    public void convertAndSend(String exchange, String routingKey, EventMessage<?> message) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
        log.info("Sent event to exchange [{}] routingKey [{}], messageId={}",
                exchange, routingKey, message.getId());
    }
}
