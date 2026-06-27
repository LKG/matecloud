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
package vip.mate.system.admin.domain.permission.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vip.mate.system.admin.domain.permission.adapter.repository.AdminRepository;
import vip.mate.system.admin.domain.permission.adapter.repository.MenuRepository;
import vip.mate.system.admin.domain.permission.adapter.repository.RoleRepository;
import vip.mate.system.admin.domain.permission.model.entity.Menu;
import vip.mate.system.admin.domain.permission.model.valobj.MenuType;
import vip.mate.system.admin.domain.permission.service.impl.PermissionDomainServiceImpl;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionDomainService tests")
class PermissionDomainServiceTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private MenuRepository menuRepository;

    @InjectMocks
    private PermissionDomainServiceImpl permissionDomainService;

    // ---- buildMenuTree tests ----

    @Test
    @DisplayName("buildMenuTree should organise flat list into parent-child tree")
    void buildMenuTree_shouldBuildCorrectTree() {
        // Root directory
        Menu root = createMenu("1", "0", "System", MenuType.DIRECTORY, 1);
        // Child menu under root
        Menu child1 = createMenu("2", "1", "Users", MenuType.MENU, 1);
        // Another child menu under root
        Menu child2 = createMenu("3", "1", "Roles", MenuType.MENU, 2);
        // Button under child1
        Menu button = createMenu("4", "2", "Add User", MenuType.BUTTON, 1);

        List<Menu> flat = new ArrayList<>(List.of(root, child1, child2, button));

        List<Menu> tree = permissionDomainService.buildMenuTree(flat);

        assertEquals(1, tree.size(), "Should have 1 root");
        Menu rootResult = tree.get(0);
        assertEquals("1", rootResult.getId());
        assertEquals(2, rootResult.getChildren().size(), "Root should have 2 children");

        Menu usersMenu = rootResult.getChildren().stream()
                .filter(m -> "2".equals(m.getId())).findFirst().orElseThrow();
        assertEquals(1, usersMenu.getChildren().size(), "Users menu should have 1 button child");
        assertEquals("4", usersMenu.getChildren().get(0).getId());

        Menu rolesMenu = rootResult.getChildren().stream()
                .filter(m -> "3".equals(m.getId())).findFirst().orElseThrow();
        assertTrue(rolesMenu.getChildren().isEmpty(), "Roles menu should have no children");
    }

    @Test
    @DisplayName("buildMenuTree with empty list should return empty")
    void buildMenuTree_emptyList_shouldReturnEmpty() {
        List<Menu> result = permissionDomainService.buildMenuTree(new ArrayList<>());
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("buildMenuTree should treat parentId='0' as root")
    void buildMenuTree_parentIdZero_isRoot() {
        Menu root1 = createMenu("1", "0", "Root1", MenuType.DIRECTORY, 2);
        Menu root2 = createMenu("2", "0", "Root2", MenuType.DIRECTORY, 1);

        List<Menu> tree = permissionDomainService.buildMenuTree(new ArrayList<>(List.of(root1, root2)));

        assertEquals(2, tree.size());
        // Should be sorted by sort field: root2 (sort=1) first, root1 (sort=2) second
        assertEquals("2", tree.get(0).getId());
        assertEquals("1", tree.get(1).getId());
    }

    @Test
    @DisplayName("buildMenuTree should treat null/blank parentId as root")
    void buildMenuTree_nullOrBlankParentId_isRoot() {
        Menu nullParent = createMenu("1", null, "NullParent", MenuType.DIRECTORY, 1);
        Menu blankParent = createMenu("2", "", "BlankParent", MenuType.DIRECTORY, 2);

        List<Menu> tree = permissionDomainService.buildMenuTree(new ArrayList<>(List.of(nullParent, blankParent)));

        assertEquals(2, tree.size());
    }

    @Test
    @DisplayName("buildMenuTree should promote orphans to root level")
    void buildMenuTree_orphanMenus_promotedToRoot() {
        // Parent "999" doesn't exist in the list
        Menu orphan = createMenu("1", "999", "Orphan", MenuType.MENU, 1);

        List<Menu> tree = permissionDomainService.buildMenuTree(new ArrayList<>(List.of(orphan)));

        assertEquals(1, tree.size(), "Orphan should be promoted to root");
        assertEquals("1", tree.get(0).getId());
    }

    // ---- findPermissionsByAdminId tests ----

    @Test
    @DisplayName("findPermissionsByAdminId should collect perms from admin -> roles -> menus chain")
    void findPermissionsByAdminId_shouldCollectPermsChain() {
        String adminId = "admin-1";
        when(adminRepository.findRoleIdsByAdminId(adminId)).thenReturn(List.of("role-1", "role-2"));
        when(roleRepository.findMenuIdsByRoleId("role-1")).thenReturn(List.of("menu-1", "menu-2"));
        when(roleRepository.findMenuIdsByRoleId("role-2")).thenReturn(List.of("menu-3"));

        Menu menu1 = createMenu("menu-1", "0", "Users", MenuType.MENU, 1);
        menu1.setPerms("sys:user:list");
        Menu menu2 = createMenu("menu-2", "0", "AddUser", MenuType.BUTTON, 2);
        menu2.setPerms("sys:user:add");
        Menu menu3 = createMenu("menu-3", "0", "Roles", MenuType.MENU, 1);
        menu3.setPerms("sys:role:list");

        when(menuRepository.findById("menu-1")).thenReturn(menu1);
        when(menuRepository.findById("menu-2")).thenReturn(menu2);
        when(menuRepository.findById("menu-3")).thenReturn(menu3);

        List<String> perms = permissionDomainService.findPermissionsByAdminId(adminId);

        assertEquals(3, perms.size());
        assertTrue(perms.contains("sys:user:list"));
        assertTrue(perms.contains("sys:user:add"));
        assertTrue(perms.contains("sys:role:list"));
    }

    @Test
    @DisplayName("findPermissionsByAdminId should skip menus with null/blank perms")
    void findPermissionsByAdminId_shouldSkipNullBlankPerms() {
        String adminId = "admin-1";
        when(adminRepository.findRoleIdsByAdminId(adminId)).thenReturn(List.of("role-1"));
        when(roleRepository.findMenuIdsByRoleId("role-1")).thenReturn(List.of("menu-1", "menu-2", "menu-3"));

        Menu menuWithPerms = createMenu("menu-1", "0", "Users", MenuType.MENU, 1);
        menuWithPerms.setPerms("sys:user:list");
        Menu menuNullPerms = createMenu("menu-2", "0", "Dir", MenuType.DIRECTORY, 2);
        menuNullPerms.setPerms(null);
        Menu menuBlankPerms = createMenu("menu-3", "0", "Blank", MenuType.DIRECTORY, 3);
        menuBlankPerms.setPerms("   ");

        when(menuRepository.findById("menu-1")).thenReturn(menuWithPerms);
        when(menuRepository.findById("menu-2")).thenReturn(menuNullPerms);
        when(menuRepository.findById("menu-3")).thenReturn(menuBlankPerms);

        List<String> perms = permissionDomainService.findPermissionsByAdminId(adminId);

        assertEquals(1, perms.size());
        assertEquals("sys:user:list", perms.get(0));
    }

    @Test
    @DisplayName("findPermissionsByAdminId should deduplicate perms across roles")
    void findPermissionsByAdminId_shouldDeduplicatePerms() {
        String adminId = "admin-1";
        when(adminRepository.findRoleIdsByAdminId(adminId)).thenReturn(List.of("role-1", "role-2"));
        when(roleRepository.findMenuIdsByRoleId("role-1")).thenReturn(List.of("menu-1"));
        when(roleRepository.findMenuIdsByRoleId("role-2")).thenReturn(List.of("menu-1")); // same menu

        Menu menu = createMenu("menu-1", "0", "Users", MenuType.MENU, 1);
        menu.setPerms("sys:user:list");
        when(menuRepository.findById("menu-1")).thenReturn(menu);

        List<String> perms = permissionDomainService.findPermissionsByAdminId(adminId);

        assertEquals(1, perms.size(), "Duplicate perms should be deduplicated");
        assertEquals("sys:user:list", perms.get(0));
    }

    @Test
    @DisplayName("findPermissionsByAdminId should return empty when admin has no roles")
    void findPermissionsByAdminId_noRoles_returnsEmpty() {
        when(adminRepository.findRoleIdsByAdminId("admin-1")).thenReturn(List.of());

        List<String> perms = permissionDomainService.findPermissionsByAdminId("admin-1");

        assertTrue(perms.isEmpty());
    }

    @Test
    @DisplayName("findPermissionsByAdminId should handle null menu from repository")
    void findPermissionsByAdminId_nullMenu_skipped() {
        String adminId = "admin-1";
        when(adminRepository.findRoleIdsByAdminId(adminId)).thenReturn(List.of("role-1"));
        when(roleRepository.findMenuIdsByRoleId("role-1")).thenReturn(List.of("menu-1"));
        when(menuRepository.findById("menu-1")).thenReturn(null);

        List<String> perms = permissionDomainService.findPermissionsByAdminId(adminId);

        assertTrue(perms.isEmpty());
    }

    // ---- helper ----

    private Menu createMenu(String id, String parentId, String name, MenuType type, Integer sort) {
        Menu menu = Menu.builder()
                .id(id)
                .parentId(parentId)
                .name(name)
                .type(type)
                .sort(sort)
                .children(new ArrayList<>())
                .build();
        return menu;
    }
}
