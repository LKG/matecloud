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
import vip.mate.base.result.PageResult;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vip.mate.system.admin.domain.permission.adapter.repository.AdminRepository;
import vip.mate.system.admin.domain.permission.model.aggregate.AdminAggregate;
import vip.mate.system.admin.domain.permission.model.entity.Admin;
import vip.mate.system.admin.domain.permission.model.valobj.AdminStatus;
import vip.mate.system.admin.infrastructure.dao.AdminDao;
import vip.mate.system.admin.infrastructure.dao.AdminRoleDao;
import vip.mate.system.admin.infrastructure.dao.po.AdminPO;
import vip.mate.system.admin.infrastructure.dao.po.AdminRolePO;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AdminRepositoryImpl implements AdminRepository {

    private final AdminDao adminDao;
    private final AdminRoleDao adminRoleDao;

    @Override
    @Transactional
    public void save(AdminAggregate agg) {
        AdminPO po = toPO(agg.getAdmin());
        adminDao.insert(po);
    }

    @Override
    @Transactional
    public void update(AdminAggregate agg) {
        AdminPO po = toPO(agg.getAdmin());
        adminDao.updateById(po);
    }

    @Override
    @Transactional
    public void delete(String id) {
        // MyBatis-Plus @TableLogic → UPDATE mate_admin SET deleted=1 WHERE id=?
        adminDao.deleteById(id);
    }

    @Override
    public AdminAggregate findById(String id) {
        AdminPO po = adminDao.selectById(id);
        if (po == null) return null;
        List<String> roleIds = adminRoleDao.selectRoleIdsByAdminId(id);
        return AdminAggregate.builder().admin(toEntity(po)).roleIds(roleIds).build();
    }

    @Override
    public AdminAggregate findByUsername(String username) {
        AdminPO po = adminDao.selectByUsername(username);
        if (po == null) return null;
        List<String> roleIds = adminRoleDao.selectRoleIdsByAdminId(po.getId());
        return AdminAggregate.builder().admin(toEntity(po)).roleIds(roleIds).build();
    }

    @Override
    public boolean existsByUsername(String username) {
        return adminDao.existsByUsername(username);
    }

    @Override
    public PageResult<AdminAggregate> pageQuery(String keyword, int pageNum, int pageSize) {
        Page<AdminPO> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AdminPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdminPO::getDeleted, 0);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w
                    .like(AdminPO::getUsername, keyword)
                    .or().like(AdminPO::getNickName, keyword));
        }
        wrapper.orderByDesc(AdminPO::getCreatedAt);
        Page<AdminPO> result = adminDao.selectPage(page, wrapper);
        List<AdminAggregate> list = result.getRecords().stream()
                .map(po -> AdminAggregate.builder()
                        .admin(toEntity(po))
                        .roleIds(adminRoleDao.selectRoleIdsByAdminId(po.getId()))
                        .build())
                .toList();
        return PageResult.of(list, result.getTotal());
    }

    @Override
    @Transactional
    public void assignRoles(String adminId, List<String> roleIds) {
        adminRoleDao.deleteByAdminId(adminId);
        for (String roleId : roleIds) {
            adminRoleDao.insert(new AdminRolePO(adminId, roleId));
        }
    }

    @Override
    public List<String> findRoleIdsByAdminId(String adminId) {
        return adminRoleDao.selectRoleIdsByAdminId(adminId);
    }

    private AdminPO toPO(Admin a) {
        AdminPO po = new AdminPO();
        po.setId(a.getId());
        po.setUsername(a.getUsername());
        po.setPassword(a.getPassword());
        po.setMobile(a.getMobile());
        po.setEmail(a.getEmail());
        po.setNickName(a.getNickName());
        po.setRealName(a.getRealName());
        po.setAvatar(a.getAvatar());
        po.setStatus(a.getStatus() != null ? a.getStatus().getCode() : 0);
        po.setDeptId(a.getDeptId());
        po.setTenantId(a.getTenantId());
        return po;
    }

    private Admin toEntity(AdminPO po) {
        return Admin.builder()
                .id(po.getId())
                .username(po.getUsername())
                .password(po.getPassword())
                .mobile(po.getMobile())
                .email(po.getEmail())
                .nickName(po.getNickName())
                .realName(po.getRealName())
                .avatar(po.getAvatar())
                .status(AdminStatus.fromCode(po.getStatus()))
                .deptId(po.getDeptId())
                .tenantId(po.getTenantId())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
}
