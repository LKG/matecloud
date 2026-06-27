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
package vip.mate.system.admin.domain.permission.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import vip.mate.base.model.entity.BaseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Department domain entity — a self-referencing hierarchy used both for org
 * management and as the anchor of data-permission (data scope) filtering.
 *
 * <p>{@code ancestors} is a comma-separated chain of ancestor ids (root first),
 * enabling subtree queries via {@code ancestors LIKE '...,deptId,%'}.
 *
 * @author mateaix
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Dept extends BaseEntity {

    public static final String ROOT = "0";

    private String parentId;
    private String deptName;
    private String ancestors;
    private Integer sort;
    private String leader;
    private String phone;
    private String email;
    /** 0 = active, 1 = disabled. */
    private Integer status;

    /** Transient children, populated when building the tree. */
    private transient List<Dept> children;

    public static Dept create(String parentId, String deptName, String ancestors,
                              Integer sort, String leader, String phone, String email) {
        return Dept.builder()
                .id(UUID.randomUUID().toString().replace("-", ""))
                .parentId(parentId == null || parentId.isBlank() ? ROOT : parentId)
                .deptName(deptName)
                .ancestors(ancestors)
                .sort(sort == null ? 0 : sort)
                .leader(leader)
                .phone(phone)
                .email(email)
                .status(0)
                .children(new ArrayList<>())
                .build();
    }
}
