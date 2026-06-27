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
package vip.mate.base.model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Framework-free base for domain entities / aggregate roots.
 *
 * <p>Carries only what is meaningful to the <em>domain</em>: stable identity and
 * audit timestamps. Pure persistence concerns — soft-delete flag, optimistic-lock
 * version, table/column mapping, id-generation strategy — deliberately live on the
 * infrastructure {@code *PO} classes (see {@code vip.mate.starter.ds.model.BasePO}),
 * NOT here. That keeps {@code mate-base} (and therefore every domain layer that
 * extends this) free of any ORM dependency, honouring the DDD rule "domain layer
 * has ZERO framework deps".
 *
 * <p>Timestamps use {@link LocalDateTime} (no embedded time zone): the wire format
 * is applied centrally by the platform Jackson config ({@code yyyy-MM-dd HH:mm:ss}),
 * so no per-field {@code @JsonFormat}/time-zone is hard-coded here.
 *
 * @author mateaix
 */
@Data
@SuperBuilder
@NoArgsConstructor
public abstract class BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String id;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
