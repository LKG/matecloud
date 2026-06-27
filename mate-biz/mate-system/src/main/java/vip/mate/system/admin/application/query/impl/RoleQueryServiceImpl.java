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
package vip.mate.system.admin.application.query.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vip.mate.system.admin.application.query.IRoleQueryService;
import vip.mate.system.admin.domain.permission.adapter.repository.RoleRepository;
import vip.mate.system.admin.domain.permission.model.aggregate.RoleAggregate;
import vip.mate.system.admin.domain.permission.model.entity.Role;
import vip.mate.base.result.PageResult;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleQueryServiceImpl implements IRoleQueryService {

    private final RoleRepository roleRepository;

    @Override
    public List<RoleVO> findAll() {
        return roleRepository.findAll().stream().map(this::toVO).toList();
    }

    @Override
    public PageResult<RoleVO> page(int pageNum, int pageSize, String keyword) {
        PageResult<RoleAggregate> raw = roleRepository.page(pageNum, pageSize, keyword);
        List<RoleVO> rows = raw.getList().stream().map(this::toVO).toList();
        return PageResult.of(rows, raw.getTotal());
    }

    @Override
    public RoleVO findById(String id) {
        RoleAggregate agg = roleRepository.findById(id);
        return agg == null ? null : toVO(agg);
    }

    private RoleVO toVO(RoleAggregate agg) {
        Role r = agg.getRole();
        return new RoleVO(r.getId(), r.getRoleKey(), r.getRoleName(),
                r.getSort(), statusName(r.getStatus()),
                r.getDataScope() == null ? 1 : r.getDataScope(), r.getCustomDeptIds(),
                agg.getMenuIds());
    }

    /** 0 (or null) = active. Maps to the same names Admin/User expose to the UI. */
    private static String statusName(Integer status) {
        return (status == null || status == 0) ? "ACTIVE" : "DISABLED";
    }
}
