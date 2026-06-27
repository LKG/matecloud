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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vip.mate.system.admin.domain.dict.adapter.repository.DictTypeRepository;
import vip.mate.system.admin.domain.dict.model.entity.DictType;
import vip.mate.system.admin.infrastructure.adapter.repository.convertor.DictTypeInfraConvertor;
import vip.mate.system.admin.infrastructure.dao.DictTypeDao;
import vip.mate.system.admin.infrastructure.dao.po.DictTypePO;
import vip.mate.base.result.PageResult;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class DictTypeRepositoryImpl implements DictTypeRepository {

    private final DictTypeDao dictTypeDao;

    @Override
    public void save(DictType dictType) {
        DictTypePO po = DictTypeInfraConvertor.INSTANCE.toPO(dictType);
        dictTypeDao.insert(po);
        dictType.setId(po.getId());
    }

    @Override
    public void update(DictType dictType) {
        DictTypePO po = DictTypeInfraConvertor.INSTANCE.toPO(dictType);
        dictTypeDao.updateById(po);
    }

    @Override
    public void deleteById(String id) {
        dictTypeDao.deleteById(id);
    }

    @Override
    public DictType findById(String id) {
        return toDomain(dictTypeDao.selectById(id));
    }

    @Override
    public DictType findByCode(String code) {
        return toDomain(dictTypeDao.selectByCode(code));
    }

    @Override
    public boolean existsByCode(String code) {
        return dictTypeDao.existsByCode(code);
    }

    @Override
    public PageResult<DictType> page(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<DictTypePO> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(DictTypePO::getDictName, keyword)
                    .or().like(DictTypePO::getDictType, keyword));
        }
        wrapper.orderByDesc(DictTypePO::getCreatedAt);

        Page<DictTypePO> page = dictTypeDao.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<DictType> rows = page.getRecords().stream().map(this::toDomain).toList();
        return PageResult.of(rows, page.getTotal());
    }

    private DictType toDomain(DictTypePO po) {
        return DictTypeInfraConvertor.INSTANCE.toEntity(po);
    }
}
