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
package vip.mate.starter.mq.delay;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Delay queue service backed by Redisson {@link RDelayedQueue} + {@link RBlockingQueue}.
 *
 * @author mateaix
 */
@Slf4j
public class DelayQueueService {

    private static final String QUEUE_PREFIX = "mate:delay:queue:";

    private final RedissonClient redissonClient;
    private final Map<String, RDelayedQueue<?>> delayedQueueMap = new ConcurrentHashMap<>();
    private final Map<String, RBlockingQueue<?>> blockingQueueMap = new ConcurrentHashMap<>();

    public DelayQueueService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public <T> String offer(String topic, T data, long delaySeconds) {
        String messageId = UUID.randomUUID().toString().replace("-", "");
        DelayQueueMessage<T> message = DelayQueueMessage.<T>builder()
                .messageId(messageId)
                .topic(topic)
                .data(data)
                .createdAt(LocalDateTime.now())
                .executeAt(LocalDateTime.now().plusSeconds(delaySeconds))
                .build();

        RBlockingQueue<DelayQueueMessage<T>> blockingQueue = getBlockingQueue(topic);
        RDelayedQueue<DelayQueueMessage<T>> delayedQueue = getDelayedQueue(topic, blockingQueue);
        delayedQueue.offer(message, delaySeconds, TimeUnit.SECONDS);

        log.info("[DelayQueue] Offered: topic={}, messageId={}, delaySeconds={}",
                topic, messageId, delaySeconds);
        return messageId;
    }

    public <T> String offer(String topic, T data, long delay, TimeUnit timeUnit) {
        return offer(topic, data, timeUnit.toSeconds(delay));
    }

    @SuppressWarnings("unchecked")
    public <T> void consume(String topic, Consumer<T> handler) {
        RBlockingQueue<DelayQueueMessage<T>> blockingQueue =
                (RBlockingQueue<DelayQueueMessage<T>>) (RBlockingQueue<?>) getBlockingQueue(topic);

        Thread consumerThread = new Thread(() -> {
            log.info("[DelayQueue] Consumer started for topic={}", topic);
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    DelayQueueMessage<T> message = blockingQueue.take();
                    try {
                        handler.accept(message.getData());
                    } catch (Exception e) {
                        log.error("[DelayQueue] Handler failed: topic={}, messageId={}",
                                topic, message.getMessageId(), e);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "delay-queue-consumer-" + topic);

        consumerThread.setDaemon(true);
        consumerThread.start();
    }

    public int size(String topic) {
        return getBlockingQueue(topic).size();
    }

    @SuppressWarnings("unchecked")
    private <T> RBlockingQueue<DelayQueueMessage<T>> getBlockingQueue(String topic) {
        return (RBlockingQueue<DelayQueueMessage<T>>) blockingQueueMap.computeIfAbsent(
                topic, t -> redissonClient.getBlockingQueue(QUEUE_PREFIX + t));
    }

    @SuppressWarnings("unchecked")
    private <T> RDelayedQueue<DelayQueueMessage<T>> getDelayedQueue(
            String topic, RBlockingQueue<DelayQueueMessage<T>> blockingQueue) {
        return (RDelayedQueue<DelayQueueMessage<T>>) delayedQueueMap.computeIfAbsent(
                topic, t -> redissonClient.getDelayedQueue(blockingQueue));
    }
}
