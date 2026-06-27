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
package vip.mate.system.admin.application.query.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vip.mate.system.admin.application.query.IMenuQueryService;
import vip.mate.system.admin.domain.permission.adapter.repository.MenuRepository;
import vip.mate.system.admin.domain.permission.model.entity.Menu;
import vip.mate.system.admin.domain.permission.service.IPermissionDomainService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuQueryServiceImpl implements IMenuQueryService {

    private final MenuRepository menuRepository;
    private final IPermissionDomainService permissionDomainService;

    @Override
    public List<Menu> findTree() {
        List<Menu> all = menuRepository.findAll();
        return permissionDomainService.buildMenuTree(all);
    }
}
