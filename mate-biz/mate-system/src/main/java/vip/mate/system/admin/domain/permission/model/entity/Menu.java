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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import vip.mate.system.admin.domain.permission.model.valobj.MenuType;
import vip.mate.base.model.entity.BaseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Menu domain entity.
 *
 * @author mateaix
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Menu extends BaseEntity {

    private String parentId;
    private String name;
    /** English menu name for i18n. Nullable — falls back to {@link #name}. */
    private String nameEn;
    /**
     * Optional short name (e.g. "系统") used by narrow rail layouts where the
     * full {@link #name} would be truncated. Null falls back to {@link #name}.
     */
    private String shortName;
    private String path;
    private String component;
    private String perms;
    private MenuType type;
    private String icon;
    private Integer sort;

    /**
     * Transient children list, populated when building menu tree.
     */
    private transient List<Menu> children;

    /**
     * Factory method to create a new Menu.
     */
    public static Menu create(String parentId, String name, String nameEn, String path,
                              String component, String perms, MenuType type,
                              String icon, Integer sort) {
        return Menu.builder()
                .id(UUID.randomUUID().toString().replace("-", ""))
                .parentId(parentId)
                .name(name)
                .nameEn(nameEn)
                .path(path)
                .component(component)
                .perms(perms)
                .type(type)
                .icon(icon)
                .sort(sort)
                .children(new ArrayList<>())
                .build();
    }

    public boolean isDirectory() {
        return type == MenuType.DIRECTORY;
    }

    public boolean isMenu() {
        return type == MenuType.MENU;
    }

    public boolean isButton() {
        return type == MenuType.BUTTON;
    }
}
