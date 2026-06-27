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
package vip.mate.system.admin.domain.permission.adapter.repository;

import vip.mate.system.admin.domain.permission.model.entity.Dept;

import java.util.List;

public interface DeptRepository {

    void save(Dept dept);

    void update(Dept dept);

    void deleteById(String id);

    Dept findById(String id);

    List<Dept> findAll();

    /** True if the given department has at least one (non-deleted) child. */
    boolean hasChildren(String parentId);
}
