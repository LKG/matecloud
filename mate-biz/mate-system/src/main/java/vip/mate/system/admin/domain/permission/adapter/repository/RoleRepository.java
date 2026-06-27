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

import vip.mate.system.admin.domain.permission.model.aggregate.RoleAggregate;
import vip.mate.base.result.PageResult;

import java.util.List;

public interface RoleRepository {
    void save(RoleAggregate aggregate);
    void update(RoleAggregate aggregate);
    void deleteById(String id);
    RoleAggregate findById(String id);
    boolean existsByKey(String roleKey);
    /** Full list — kept for select-options style consumers (e.g. assign-roles dialog). */
    List<RoleAggregate> findAll();
    /**
     * Paginated list used by the role management page. {@code keyword} matches
     * both {@code role_key} and {@code role_name}.
     */
    PageResult<RoleAggregate> page(int pageNum, int pageSize, String keyword);
    void assignMenus(String roleId, List<String> menuIds);
    List<String> findMenuIdsByRoleId(String roleId);
}
