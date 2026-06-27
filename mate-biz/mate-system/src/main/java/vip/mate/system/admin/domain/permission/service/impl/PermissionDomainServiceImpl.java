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
package vip.mate.system.admin.domain.permission.service.impl;

import lombok.RequiredArgsConstructor;
import vip.mate.system.admin.domain.permission.adapter.repository.AdminRepository;
import vip.mate.system.admin.domain.permission.adapter.repository.MenuRepository;
import vip.mate.system.admin.domain.permission.adapter.repository.RoleRepository;
import vip.mate.system.admin.domain.permission.model.aggregate.RoleAggregate;
import vip.mate.system.admin.domain.permission.model.entity.Menu;
import vip.mate.system.admin.domain.permission.service.IPermissionDomainService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Framework-free domain service (no Spring annotations) — registered as a bean by
 * {@code AdminDomainServiceConfiguration} in the infrastructure layer.
 *
 * @author mateaix
 */
@RequiredArgsConstructor
public class PermissionDomainServiceImpl implements IPermissionDomainService {

    private final AdminRepository adminRepository;
    private final RoleRepository roleRepository;
    private final MenuRepository menuRepository;

    @Override
    public List<String> findPermissionsByAdminId(String adminId) {
        List<String> roleIds = adminRepository.findRoleIdsByAdminId(adminId);
        Set<String> perms = new LinkedHashSet<>();
        for (String roleId : roleIds) {
            List<String> menuIds = roleRepository.findMenuIdsByRoleId(roleId);
            for (String menuId : menuIds) {
                Menu menu = menuRepository.findById(menuId);
                if (menu != null && menu.getPerms() != null && !menu.getPerms().isBlank()) {
                    perms.add(menu.getPerms());
                }
            }
        }
        return new ArrayList<>(perms);
    }

    @Override
    public List<String> findRoleKeysByAdminId(String adminId) {
        List<String> roleIds = adminRepository.findRoleIdsByAdminId(adminId);
        return roleIds.stream()
                .map(roleRepository::findById)
                .filter(Objects::nonNull)
                .map(r -> r.getRole().getRoleKey())
                .toList();
    }

    @Override
    public List<String> findPermissionsByUsername(String username) {
        var admin = adminRepository.findByUsername(username);
        if (admin == null) return List.of();
        return findPermissionsByAdminId(admin.getId());
    }

    @Override
    public List<String> findRoleKeysByUsername(String username) {
        var admin = adminRepository.findByUsername(username);
        if (admin == null) return List.of();
        return findRoleKeysByAdminId(admin.getId());
    }

    @Override
    public List<Menu> buildMenuTree(List<Menu> menus) {
        Map<String, Menu> map = menus.stream()
                .peek(m -> { if (m.getChildren() == null) m.setChildren(new ArrayList<>()); })
                .collect(Collectors.toMap(Menu::getId, m -> m, (a, b) -> a, LinkedHashMap::new));

        List<Menu> roots = new ArrayList<>();
        for (Menu menu : menus) {
            if (menu.getParentId() == null || menu.getParentId().isBlank() || "0".equals(menu.getParentId())) {
                roots.add(menu);
            } else {
                Menu parent = map.get(menu.getParentId());
                if (parent != null) {
                    parent.getChildren().add(menu);
                } else {
                    roots.add(menu);
                }
            }
        }
        roots.sort(Comparator.comparing(m -> m.getSort() == null ? 0 : m.getSort()));
        return roots;
    }
}
