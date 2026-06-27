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
package vip.mate.system.admin.domain.dict.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import vip.mate.base.model.entity.BaseEntity;

/**
 * DictType — top-level dictionary category (e.g. {@code sys_status},
 * {@code sys_gender}). Holds a stable {@code dictType} code plus a human
 * readable {@code dictName}; {@link DictData} entries reference it by
 * {@code dictType} string (loose coupling — same as the database schema in
 * mate_admin/V1__admin_schema.sql).
 *
 * <p>Kept as a separate aggregate root rather than nested under DictData so
 * that frontends with two-pane editors (RFC-046 DictManager) can list types
 * and data independently and the type catalog can be cached on its own.
 *
 * @author mateaix
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DictType extends BaseEntity {

    /** Stable code referenced by DictData rows. Immutable after creation. */
    private String dictType;

    /** Human-readable category name shown in the admin UI. */
    private String dictName;

    /** Optional remark / description. */
    private String remark;

    /** 0 = enabled, 1 = disabled. */
    private Integer status;

    public void enable() { this.status = 1; }

    public void disable() { this.status = 0; }
}
