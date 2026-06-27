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
import vip.mate.system.admin.domain.permission.adapter.repository.MenuRepository;
import vip.mate.system.admin.domain.permission.model.entity.Menu;
import vip.mate.system.admin.domain.permission.model.valobj.MenuType;
import vip.mate.system.admin.types.exception.AdminErrorCode;
import vip.mate.base.exception.BizException;

@Service
@RequiredArgsConstructor
public class MenuCommandService {

    private final MenuRepository menuRepository;

    @Transactional
    public String createMenu(String parentId, String name, String nameEn, String shortName,
                             String path, String component, String perms, String type,
                             String icon, Integer sort) {
        MenuType menuType = MenuType.MENU;
        for (MenuType t : MenuType.values()) {
            if (t.getCode().equals(type)) { menuType = t; break; }
        }
        Menu menu = Menu.create(parentId, name, nameEn, path, component, perms, menuType, icon, sort);
        // Set after create() to keep the existing 9-arg factory signature (and its tests) intact.
        menu.setShortName(shortName);
        menuRepository.save(menu);
        return menu.getId();
    }

    @Transactional
    public void updateMenu(String id, String name, String nameEn, String shortName,
                           String path, String component, String perms, String icon, Integer sort) {
        Menu menu = menuRepository.findById(id);
        if (menu == null) throw BizException.of(AdminErrorCode.MENU_NOT_EXIST);
        menu.setName(name);
        menu.setNameEn(nameEn);
        menu.setShortName(shortName);
        menu.setPath(path);
        menu.setComponent(component);
        menu.setPerms(perms);
        menu.setIcon(icon);
        menu.setSort(sort);
        menuRepository.update(menu);
    }

    @Transactional
    public void deleteMenu(String id) {
        if (menuRepository.findById(id) == null) throw BizException.of(AdminErrorCode.MENU_NOT_EXIST);
        menuRepository.deleteById(id);
    }
}
