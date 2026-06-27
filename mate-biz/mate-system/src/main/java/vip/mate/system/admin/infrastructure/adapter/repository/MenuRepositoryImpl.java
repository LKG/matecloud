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
import vip.mate.system.admin.domain.permission.adapter.repository.MenuRepository;
import vip.mate.system.admin.domain.permission.model.entity.Menu;
import vip.mate.system.admin.domain.permission.model.valobj.MenuType;
import vip.mate.system.admin.infrastructure.dao.MenuDao;
import vip.mate.system.admin.infrastructure.dao.po.MenuPO;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MenuRepositoryImpl implements MenuRepository {

    private final MenuDao menuDao;

    @Override
    public void save(Menu menu) { menuDao.insert(toPO(menu)); }

    @Override
    public void update(Menu menu) { menuDao.updateById(toPO(menu)); }

    @Override
    public Menu findById(String id) {
        MenuPO po = menuDao.selectById(id);
        return po == null ? null : toEntity(po);
    }

    @Override
    public void deleteById(String id) { menuDao.deleteById(id); }

    @Override
    public List<Menu> findAll() {
        return menuDao.selectList(new LambdaQueryWrapper<MenuPO>()
                        .eq(MenuPO::getDeleted, 0).orderByAsc(MenuPO::getSort))
                .stream().map(this::toEntity).toList();
    }

    private MenuPO toPO(Menu m) {
        MenuPO po = new MenuPO();
        po.setId(m.getId()); po.setParentId(m.getParentId()); po.setName(m.getName());
        po.setNameEn(m.getNameEn()); po.setShortName(m.getShortName());
        po.setPath(m.getPath()); po.setComponent(m.getComponent()); po.setPerms(m.getPerms());
        po.setType(m.getType() != null ? m.getType().getCode() : "C");
        po.setIcon(m.getIcon()); po.setSort(m.getSort());
        return po;
    }

    private Menu toEntity(MenuPO po) {
        MenuType type = null;
        for (MenuType t : MenuType.values()) {
            if (t.getCode().equals(po.getType())) { type = t; break; }
        }
        return Menu.builder()
                .id(po.getId()).parentId(po.getParentId()).name(po.getName()).nameEn(po.getNameEn())
                .shortName(po.getShortName())
                .path(po.getPath()).component(po.getComponent()).perms(po.getPerms())
                .type(type).icon(po.getIcon()).sort(po.getSort())
                .createdAt(po.getCreatedAt()).updatedAt(po.getUpdatedAt())
                .build();
    }
}
