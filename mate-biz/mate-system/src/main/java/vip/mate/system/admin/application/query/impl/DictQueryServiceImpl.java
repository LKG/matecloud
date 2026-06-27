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
import vip.mate.system.admin.application.query.IDictQueryService;
import vip.mate.system.admin.domain.dict.adapter.repository.DictRepository;
import vip.mate.system.admin.domain.dict.adapter.repository.DictTypeRepository;
import vip.mate.system.admin.domain.dict.model.entity.DictData;
import vip.mate.system.admin.domain.dict.model.entity.DictType;
import vip.mate.base.result.PageResult;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DictQueryServiceImpl implements IDictQueryService {

    private final DictRepository dictRepository;
    private final DictTypeRepository dictTypeRepository;

    // ---- DictData ----

    @Override
    public DictData findById(String id) {
        return dictRepository.findById(id);
    }

    @Override
    public List<DictData> findByType(String dictType) {
        return dictRepository.findByType(dictType);
    }

    @Override
    public String findLabel(String dictType, String dictValue) {
        return dictRepository.findLabel(dictType, dictValue);
    }

    @Override
    public PageResult<DictData> pageData(String dictType, int pageNum, int pageSize, String keyword) {
        return dictRepository.page(dictType, pageNum, pageSize, keyword);
    }

    // ---- DictType ----

    @Override
    public DictType findTypeById(String id) {
        return dictTypeRepository.findById(id);
    }

    @Override
    public DictType findTypeByCode(String code) {
        return dictTypeRepository.findByCode(code);
    }

    @Override
    public PageResult<DictType> pageTypes(int pageNum, int pageSize, String keyword) {
        return dictTypeRepository.page(pageNum, pageSize, keyword);
    }
}
