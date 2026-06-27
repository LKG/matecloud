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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.mate.system.admin.domain.dict.adapter.repository.DictRepository;
import vip.mate.system.admin.domain.dict.adapter.repository.DictTypeRepository;
import vip.mate.system.admin.domain.dict.model.entity.DictData;
import vip.mate.system.admin.domain.dict.model.entity.DictType;
import vip.mate.system.admin.types.exception.AdminErrorCode;
import vip.mate.base.exception.BizException;

@Slf4j
@Service
@RequiredArgsConstructor
public class DictCommandService {

    private final DictRepository dictRepository;
    private final DictTypeRepository dictTypeRepository;

    // ---------- DictData ----------

    @Transactional(rollbackFor = Exception.class)
    public String create(DictData dictData) {
        // Reject orphan rows: the DictType referenced by `dictType` (loose
        // string FK, not a DB constraint) must already exist. Without this
        // check the UI would happily insert rows that never appear under
        // any type's data pane.
        if (dictData.getDictType() == null
                || !dictTypeRepository.existsByCode(dictData.getDictType())) {
            throw BizException.of(AdminErrorCode.DICT_TYPE_NOT_EXIST);
        }
        dictRepository.save(dictData);
        return dictData.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(DictData dictData) {
        // dictType swap is allowed (admin moves an entry under a different
        // type) but only to an existing type.
        if (dictData.getDictType() != null
                && !dictTypeRepository.existsByCode(dictData.getDictType())) {
            throw BizException.of(AdminErrorCode.DICT_TYPE_NOT_EXIST);
        }
        dictRepository.update(dictData);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        dictRepository.deleteById(id);
    }

    // ---------- DictType ----------

    @Transactional(rollbackFor = Exception.class)
    public String createType(DictType type) {
        if (dictTypeRepository.existsByCode(type.getDictType())) {
            throw BizException.of(AdminErrorCode.DUPLICATE_DICT_TYPE_CODE);
        }
        if (type.getStatus() == null) {
            type.setStatus(1);
        }
        dictTypeRepository.save(type);
        return type.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateType(String id, DictType patch) {
        DictType existing = dictTypeRepository.findById(id);
        if (existing == null) {
            throw BizException.of(AdminErrorCode.DICT_TYPE_NOT_EXIST);
        }
        // dictType code is immutable — only name/remark/status can be changed
        if (patch.getDictName() != null) existing.setDictName(patch.getDictName());
        if (patch.getRemark() != null) existing.setRemark(patch.getRemark());
        if (patch.getStatus() != null) existing.setStatus(patch.getStatus());
        dictTypeRepository.update(existing);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteType(String id) {
        DictType existing = dictTypeRepository.findById(id);
        if (existing == null) {
            throw BizException.of(AdminErrorCode.DICT_TYPE_NOT_EXIST);
        }
        dictTypeRepository.deleteById(id);
        // DictData rows are NOT cascaded — by design they reference the type
        // code, not the type id. Cascade should be opt-in via a separate command.
    }
}
