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
import vip.mate.system.admin.application.query.IDeptQueryService;
import vip.mate.system.admin.domain.permission.adapter.repository.DeptRepository;
import vip.mate.system.admin.domain.permission.model.entity.Dept;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeptQueryServiceImpl implements IDeptQueryService {

    private final DeptRepository deptRepository;

    @Override
    public List<Dept> findAll() {
        return deptRepository.findAll();
    }

    @Override
    public List<Dept> findTree() {
        List<Dept> all = deptRepository.findAll();
        Map<String, Dept> byId = new LinkedHashMap<>();
        for (Dept d : all) {
            d.setChildren(new ArrayList<>());
            byId.put(d.getId(), d);
        }
        List<Dept> roots = new ArrayList<>();
        for (Dept d : all) {
            Dept parent = byId.get(d.getParentId());
            if (parent != null) {
                parent.getChildren().add(d);
            } else {
                roots.add(d);
            }
        }
        return roots;
    }
}
