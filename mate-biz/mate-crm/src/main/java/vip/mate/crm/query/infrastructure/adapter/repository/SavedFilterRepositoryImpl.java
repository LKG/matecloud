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
package vip.mate.crm.query.infrastructure.adapter.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vip.mate.crm.query.domain.adapter.repository.ISavedFilterRepository;
import vip.mate.crm.query.domain.model.SavedFilter;
import vip.mate.crm.query.infrastructure.dao.SavedFilterDao;
import vip.mate.crm.query.infrastructure.dao.po.SavedFilterPO;

import java.util.List;

/** 个人常用筛选仓储实现: 增删改按 id 单行操作(不做删后重插, 规避 @TableLogic 主键冲突)。 */
@Repository
@RequiredArgsConstructor
public class SavedFilterRepositoryImpl implements ISavedFilterRepository {

    private final SavedFilterDao savedFilterDao;

    @Override
    public List<SavedFilter> listByUser(String userId, String moduleKey) {
        LambdaQueryWrapper<SavedFilterPO> wrapper = new LambdaQueryWrapper<SavedFilterPO>()
                .eq(SavedFilterPO::getUserId, userId)
                .eq(moduleKey != null && !moduleKey.isBlank(), SavedFilterPO::getModuleKey, moduleKey)
                .orderByAsc(SavedFilterPO::getSort);
        return savedFilterDao.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    @Override
    public SavedFilter findById(String id) {
        SavedFilterPO po = savedFilterDao.selectById(id);
        return po == null ? null : toDomain(po);
    }

    @Override
    public void save(SavedFilter filter) {
        savedFilterDao.insert(toPO(filter));
    }

    @Override
    public void update(SavedFilter filter) {
        savedFilterDao.updateById(toPO(filter));
    }

    @Override
    public void deleteById(String id) {
        savedFilterDao.deleteById(id);
    }

    private SavedFilter toDomain(SavedFilterPO po) {
        return SavedFilter.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .moduleKey(po.getModuleKey())
                .name(po.getName())
                .queryJson(po.getQueryJson())
                .sort(po.getSort() == null ? 0 : po.getSort())
                .build();
    }

    private SavedFilterPO toPO(SavedFilter filter) {
        SavedFilterPO po = new SavedFilterPO();
        po.setId(filter.getId());
        po.setUserId(filter.getUserId());
        po.setModuleKey(filter.getModuleKey());
        po.setName(filter.getName());
        po.setQueryJson(filter.getQueryJson());
        po.setSort(filter.getSort());
        return po;
    }
}
