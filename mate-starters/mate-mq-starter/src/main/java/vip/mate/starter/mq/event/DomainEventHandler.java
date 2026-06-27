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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a domain event handler bound to a RabbitMQ topic exchange.
 * <p>
 * The annotated method must have exactly one parameter whose type matches the
 * deserialized event payload. The registrar creates a queue, binds it to the
 * {@code mate.domain.events} exchange, and wires retry + DLQ logic automatically.
 *
 * @author mateaix
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DomainEventHandler {

    /**
     * Topic (routing key pattern) on the {@code mate.domain.events} exchange.
     */
    String topic();

    /**
     * Consumer group name. Consumers in the same group share a single queue.
     * Defaults to {@code "default"}.
     */
    String consumerGroup() default "";

    /**
     * Maximum number of retry attempts before the message is sent to the DLQ.
     */
    int maxRetries() default 3;
}
