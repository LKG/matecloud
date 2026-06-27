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
package vip.mate.system.admin.application.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.mate.system.admin.domain.permission.adapter.repository.DeptRepository;
import vip.mate.system.admin.domain.permission.model.entity.Dept;
import vip.mate.base.exception.BizException;

@Service
@RequiredArgsConstructor
public class DeptCommandService {

    private final DeptRepository deptRepository;

    @Transactional
    public String createDept(String parentId, String deptName, Integer sort,
                             String leader, String phone, String email) {
        String ancestors = resolveAncestors(parentId);
        Dept dept = Dept.create(parentId, deptName, ancestors, sort, leader, phone, email);
        deptRepository.save(dept);
        return dept.getId();
    }

    @Transactional
    public void updateDept(String id, String deptName, Integer sort,
                           String leader, String phone, String email, Integer status) {
        Dept dept = mustFind(id);
        dept.setDeptName(deptName);
        dept.setSort(sort);
        dept.setLeader(leader);
        dept.setPhone(phone);
        dept.setEmail(email);
        if (status != null) {
            dept.setStatus(status);
        }
        deptRepository.update(dept);
    }

    @Transactional
    public void deleteDept(String id) {
        mustFind(id);
        if (deptRepository.hasChildren(id)) {
            throw new BizException("DEPT_HAS_CHILDREN", "存在子部门，无法删除");
        }
        deptRepository.deleteById(id);
    }

    /**
     * Ancestor chain = parent's ancestors + parent id. Root departments
     * (parentId blank/"0") get ancestors = {@link Dept#ROOT}.
     */
    private String resolveAncestors(String parentId) {
        if (parentId == null || parentId.isBlank() || Dept.ROOT.equals(parentId)) {
            return Dept.ROOT;
        }
        Dept parent = mustFind(parentId);
        String parentAncestors = parent.getAncestors() == null || parent.getAncestors().isBlank()
                ? Dept.ROOT : parent.getAncestors();
        return parentAncestors + "," + parentId;
    }

    private Dept mustFind(String id) {
        Dept dept = deptRepository.findById(id);
        if (dept == null) {
            throw new BizException("DEPT_NOT_EXIST", "部门不存在: " + id);
        }
        return dept;
    }
}
