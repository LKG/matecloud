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
package vip.mate.system.admin.infrastructure.adapter.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vip.mate.system.admin.domain.permission.adapter.repository.RoleRepository;
import vip.mate.system.admin.domain.permission.model.aggregate.RoleAggregate;
import vip.mate.system.admin.domain.permission.model.entity.Role;
import vip.mate.system.admin.infrastructure.dao.RoleDao;
import vip.mate.system.admin.infrastructure.dao.RoleMenuDao;
import vip.mate.system.admin.infrastructure.dao.po.RoleMenuPO;
import vip.mate.system.admin.infrastructure.dao.po.RolePO;
import vip.mate.base.result.PageResult;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class RoleRepositoryImpl implements RoleRepository {

    private final RoleDao roleDao;
    private final RoleMenuDao roleMenuDao;

    @Override
    public void save(RoleAggregate agg) {
        roleDao.insert(toPO(agg.getRole()));
    }

    @Override
    public void update(RoleAggregate agg) {
        roleDao.updateById(toPO(agg.getRole()));
    }

    @Override
    public void deleteById(String id) {
        roleDao.deleteById(id);
    }

    @Override
    public RoleAggregate findById(String id) {
        RolePO po = roleDao.selectById(id);
        if (po == null) return null;
        List<String> menuIds = roleMenuDao.selectMenuIdsByRoleId(id);
        return RoleAggregate.builder().role(toEntity(po)).menuIds(menuIds).build();
    }

    @Override
    public boolean existsByKey(String roleKey) {
        return roleDao.existsByRoleKey(roleKey);
    }

    @Override
    public List<RoleAggregate> findAll() {
        return roleDao.selectList(new LambdaQueryWrapper<RolePO>().eq(RolePO::getDeleted, 0)
                        .orderByAsc(RolePO::getSort))
                .stream()
                .map(po -> RoleAggregate.builder().role(toEntity(po))
                        .menuIds(roleMenuDao.selectMenuIdsByRoleId(po.getId())).build())
                .toList();
    }

    @Override
    public PageResult<RoleAggregate> page(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<RolePO> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(RolePO::getRoleName, keyword)
                    .or().like(RolePO::getRoleKey, keyword));
        }
        wrapper.orderByAsc(RolePO::getSort);

        Page<RolePO> page = roleDao.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<RoleAggregate> rows = page.getRecords().stream()
                .map(po -> RoleAggregate.builder().role(toEntity(po))
                        .menuIds(roleMenuDao.selectMenuIdsByRoleId(po.getId())).build())
                .toList();
        return PageResult.of(rows, page.getTotal());
    }

    @Override
    @Transactional
    public void assignMenus(String roleId, List<String> menuIds) {
        roleMenuDao.deleteByRoleId(roleId);
        for (String menuId : menuIds) {
            roleMenuDao.insert(new RoleMenuPO(roleId, menuId));
        }
    }

    @Override
    public List<String> findMenuIdsByRoleId(String roleId) {
        return roleMenuDao.selectMenuIdsByRoleId(roleId);
    }

    private RolePO toPO(Role r) {
        RolePO po = new RolePO();
        po.setId(r.getId());
        po.setRoleKey(r.getRoleKey());
        po.setRoleName(r.getRoleName());
        po.setSort(r.getSort());
        po.setStatus(r.getStatus());
        po.setDataScope(r.getDataScope());
        po.setCustomDeptIds(r.getCustomDeptIds());
        po.setTenantId(r.getTenantId());
        return po;
    }

    private Role toEntity(RolePO po) {
        return Role.builder()
                .id(po.getId()).roleKey(po.getRoleKey()).roleName(po.getRoleName())
                .sort(po.getSort()).status(po.getStatus())
                .dataScope(po.getDataScope()).customDeptIds(po.getCustomDeptIds())
                .tenantId(po.getTenantId())
                .createdAt(po.getCreatedAt()).updatedAt(po.getUpdatedAt())
                .build();
    }
}
