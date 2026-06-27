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
import vip.mate.system.admin.domain.dict.adapter.repository.DictRepository;
import vip.mate.system.admin.domain.dict.model.entity.DictData;
import vip.mate.system.admin.infrastructure.adapter.repository.convertor.DictDataInfraConvertor;
import vip.mate.system.admin.infrastructure.dao.DictDataDao;
import vip.mate.system.admin.infrastructure.dao.po.DictDataPO;
import vip.mate.base.result.PageResult;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class DictRepositoryImpl implements DictRepository {

    private final DictDataDao dictDataDao;

    @Override
    public void save(DictData dictData) {
        DictDataPO po = DictDataInfraConvertor.INSTANCE.toPO(dictData);
        dictDataDao.insert(po);
        dictData.setId(po.getId());
    }

    @Override
    public void update(DictData dictData) {
        DictDataPO po = DictDataInfraConvertor.INSTANCE.toPO(dictData);
        dictDataDao.updateById(po);
    }

    @Override
    public void deleteById(String id) {
        dictDataDao.deleteById(id);
    }

    @Override
    public DictData findById(String id) {
        DictDataPO po = dictDataDao.selectById(id);
        return toDomain(po);
    }

    @Override
    public List<DictData> findByType(String dictType) {
        return dictDataDao.selectByType(dictType).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public String findLabel(String dictType, String dictValue) {
        return dictDataDao.selectLabel(dictType, dictValue);
    }

    @Override
    public PageResult<DictData> page(String dictType, int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<DictDataPO> wrapper = new LambdaQueryWrapper<DictDataPO>()
                .eq(DictDataPO::getDictType, dictType);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(DictDataPO::getDictLabel, keyword)
                    .or().like(DictDataPO::getDictValue, keyword));
        }
        wrapper.orderByAsc(DictDataPO::getSort);

        Page<DictDataPO> page = dictDataDao.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<DictData> rows = page.getRecords().stream().map(this::toDomain).toList();
        return PageResult.of(rows, page.getTotal());
    }

    private DictData toDomain(DictDataPO po) {
        return DictDataInfraConvertor.INSTANCE.toEntity(po);
    }
}
