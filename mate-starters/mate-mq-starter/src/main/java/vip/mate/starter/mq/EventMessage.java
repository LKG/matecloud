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

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Generic event message wrapper.
 *
 * @author mateaix
 */
public class EventMessage<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String id;
    private LocalDateTime timestamp;
    private T data;

    public EventMessage() {
    }

    public EventMessage(T data) {
        this.id = UUID.randomUUID().toString().replace("-", "");
        this.timestamp = LocalDateTime.now();
        this.data = data;
    }

    public EventMessage(String id, LocalDateTime timestamp, T data) {
        this.id = id;
        this.timestamp = timestamp;
        this.data = data;
    }

    public static <T> EventMessage<T> of(T data) {
        return new EventMessage<>(data);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "EventMessage{id='" + id + "', timestamp=" + timestamp + ", data=" + data + '}';
    }
}
