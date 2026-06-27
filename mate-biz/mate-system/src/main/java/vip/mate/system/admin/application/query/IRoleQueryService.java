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
package vip.mate.system.admin.application.query;

import vip.mate.base.result.PageResult;

import java.util.List;

public interface IRoleQueryService {
    /** Full role list (used by select-all dropdowns / role-assignment dialogs). */
    List<RoleVO> findAll();

    /** Paginated role list used by the role management page. */
    PageResult<RoleVO> page(int pageNum, int pageSize, String keyword);

    RoleVO findById(String id);

    // status is a string enum name ("ACTIVE"/"DISABLED") to match Admin/User —
    // Role has no dedicated enum, the code (0=active) is mapped in the impl.
    // dataScope: 1=ALL 2=DEPT 3=DEPT_AND_CHILD 4=SELF 5=CUSTOM.
    record RoleVO(String id, String roleKey, String roleName, Integer sort,
                  String status, Integer dataScope, String customDeptIds,
                  List<String> menuIds) {}
}
