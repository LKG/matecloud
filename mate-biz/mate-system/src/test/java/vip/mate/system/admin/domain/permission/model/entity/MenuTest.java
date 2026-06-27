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
package vip.mate.system.admin.domain.permission.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import vip.mate.system.admin.domain.permission.model.valobj.MenuType;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Menu entity tests")
class MenuTest {

    @Test
    @DisplayName("create() should generate an ID and initialise children list")
    void create_shouldGenerateIdAndInitChildren() {
        Menu menu = Menu.create("0", "System", null, "/system", null,
                null, MenuType.DIRECTORY, "setting", 1);

        assertNotNull(menu.getId());
        assertEquals(32, menu.getId().length());
        assertEquals("0", menu.getParentId());
        assertEquals("System", menu.getName());
        assertEquals("/system", menu.getPath());
        assertNull(menu.getComponent());
        assertNull(menu.getPerms());
        assertEquals(MenuType.DIRECTORY, menu.getType());
        assertEquals("setting", menu.getIcon());
        assertEquals(1, menu.getSort());
        assertNotNull(menu.getChildren(), "children list should be initialised");
        assertTrue(menu.getChildren().isEmpty());
    }

    @Test
    @DisplayName("isDirectory() returns true only for DIRECTORY type")
    void isDirectory_trueForDirectory() {
        Menu dir = Menu.create("0", "Dir", null, "/dir", null, null, MenuType.DIRECTORY, null, 0);
        Menu menuItem = Menu.create("0", "Menu", null, "/menu", "comp", null, MenuType.MENU, null, 0);
        Menu btn = Menu.create("0", "Btn", null, null, null, "sys:user:add", MenuType.BUTTON, null, 0);

        assertTrue(dir.isDirectory());
        assertFalse(menuItem.isDirectory());
        assertFalse(btn.isDirectory());
    }

    @Test
    @DisplayName("isMenu() returns true only for MENU type")
    void isMenu_trueForMenu() {
        Menu dir = Menu.create("0", "Dir", null, "/dir", null, null, MenuType.DIRECTORY, null, 0);
        Menu menuItem = Menu.create("0", "Menu", null, "/menu", "comp", null, MenuType.MENU, null, 0);
        Menu btn = Menu.create("0", "Btn", null, null, null, "sys:user:add", MenuType.BUTTON, null, 0);

        assertFalse(dir.isMenu());
        assertTrue(menuItem.isMenu());
        assertFalse(btn.isMenu());
    }

    @Test
    @DisplayName("isButton() returns true only for BUTTON type")
    void isButton_trueForButton() {
        Menu dir = Menu.create("0", "Dir", null, "/dir", null, null, MenuType.DIRECTORY, null, 0);
        Menu menuItem = Menu.create("0", "Menu", null, "/menu", "comp", null, MenuType.MENU, null, 0);
        Menu btn = Menu.create("0", "Btn", null, null, null, "sys:user:add", MenuType.BUTTON, null, 0);

        assertFalse(dir.isButton());
        assertFalse(menuItem.isButton());
        assertTrue(btn.isButton());
    }

    @ParameterizedTest
    @EnumSource(MenuType.class)
    @DisplayName("MenuType codes should not be null or empty")
    void menuType_codesShouldBeNonEmpty(MenuType type) {
        assertNotNull(type.getCode());
        assertFalse(type.getCode().isEmpty());
        assertNotNull(type.getDesc());
    }

    @Test
    @DisplayName("MenuType enum has exactly 3 values: M, C, F")
    void menuType_hasExpectedCodes() {
        assertEquals("M", MenuType.DIRECTORY.getCode());
        assertEquals("C", MenuType.MENU.getCode());
        assertEquals("F", MenuType.BUTTON.getCode());
        assertEquals(3, MenuType.values().length);
    }
}
