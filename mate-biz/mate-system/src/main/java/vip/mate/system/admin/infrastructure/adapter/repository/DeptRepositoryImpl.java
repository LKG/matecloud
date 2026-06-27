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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vip.mate.system.admin.domain.permission.adapter.repository.DeptRepository;
import vip.mate.system.admin.domain.permission.model.entity.Dept;
import vip.mate.system.admin.infrastructure.dao.DeptDao;
import vip.mate.system.admin.infrastructure.dao.po.DeptPO;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class DeptRepositoryImpl implements DeptRepository {

    private final DeptDao deptDao;

    @Override
    public void save(Dept dept) {
        deptDao.insert(toPO(dept));
    }

    @Override
    public void update(Dept dept) {
        deptDao.updateById(toPO(dept));
    }

    @Override
    public void deleteById(String id) {
        deptDao.deleteById(id);
    }

    @Override
    public Dept findById(String id) {
        DeptPO po = deptDao.selectById(id);
        return po == null ? null : toEntity(po);
    }

    @Override
    public List<Dept> findAll() {
        return deptDao.selectList(new LambdaQueryWrapper<DeptPO>()
                        .eq(DeptPO::getDeleted, 0).orderByAsc(DeptPO::getSort))
                .stream().map(this::toEntity).toList();
    }

    @Override
    public boolean hasChildren(String parentId) {
        return deptDao.exists(new LambdaQueryWrapper<DeptPO>()
                .eq(DeptPO::getParentId, parentId)
                .eq(DeptPO::getDeleted, 0));
    }

    private DeptPO toPO(Dept d) {
        DeptPO po = new DeptPO();
        po.setId(d.getId());
        po.setParentId(d.getParentId());
        po.setDeptName(d.getDeptName());
        po.setAncestors(d.getAncestors());
        po.setSort(d.getSort());
        po.setLeader(d.getLeader());
        po.setPhone(d.getPhone());
        po.setEmail(d.getEmail());
        po.setStatus(d.getStatus());
        return po;
    }

    private Dept toEntity(DeptPO po) {
        return Dept.builder()
                .id(po.getId())
                .parentId(po.getParentId())
                .deptName(po.getDeptName())
                .ancestors(po.getAncestors())
                .sort(po.getSort())
                .leader(po.getLeader())
                .phone(po.getPhone())
                .email(po.getEmail())
                .status(po.getStatus())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
}
