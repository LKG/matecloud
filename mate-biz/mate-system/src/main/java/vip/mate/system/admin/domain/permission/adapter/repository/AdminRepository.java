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

import vip.mate.base.result.PageResult;
import vip.mate.system.admin.domain.permission.model.aggregate.AdminAggregate;
import java.util.List;

public interface AdminRepository {
    void save(AdminAggregate aggregate);
    void update(AdminAggregate aggregate);
    /** Logical delete (sets deleted=1 via MyBatis-Plus @TableLogic). */
    void delete(String id);
    AdminAggregate findById(String id);
    AdminAggregate findByUsername(String username);
    boolean existsByUsername(String username);
    PageResult<AdminAggregate> pageQuery(String keyword, int pageNum, int pageSize);
    void assignRoles(String adminId, List<String> roleIds);
    List<String> findRoleIdsByAdminId(String adminId);
}
