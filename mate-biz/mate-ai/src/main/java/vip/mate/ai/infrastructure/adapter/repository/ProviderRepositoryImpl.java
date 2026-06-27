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
package vip.mate.ai.infrastructure.adapter.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vip.mate.ai.domain.adapter.repository.IProviderRepository;
import vip.mate.ai.domain.model.aggregate.ProviderAggregate;
import vip.mate.ai.infrastructure.adapter.repository.convertor.ProviderInfraConvertor;
import vip.mate.ai.infrastructure.dao.ProviderDao;
import vip.mate.ai.infrastructure.dao.po.ProviderPO;
import vip.mate.base.result.PageResult;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProviderRepositoryImpl implements IProviderRepository {

    private static final ProviderInfraConvertor CONVERTOR = ProviderInfraConvertor.INSTANCE;

    private final ProviderDao providerDao;

    @Override public void save(ProviderAggregate p)        { providerDao.insert(CONVERTOR.toPO(p)); }
    @Override public void update(ProviderAggregate p)      { providerDao.updateById(CONVERTOR.toPO(p)); }
    @Override public void deleteById(String id)            { providerDao.deleteById(id); }
    @Override public ProviderAggregate findById(String id) { return CONVERTOR.toDomain(providerDao.selectById(id)); }

    @Override
    public ProviderAggregate findByCode(String code) {
        return CONVERTOR.toDomain(providerDao.selectOne(new LambdaQueryWrapper<ProviderPO>()
                .eq(ProviderPO::getCode, code).last("LIMIT 1")));
    }

    @Override
    public void clearDefaultExcept(String keepId) {
        providerDao.update(null, new LambdaUpdateWrapper<ProviderPO>()
                .set(ProviderPO::getIsDefault, 0)
                .eq(ProviderPO::getIsDefault, 1)
                .ne(keepId != null && !keepId.isBlank(), ProviderPO::getId, keepId));
    }

    @Override
    public PageResult<ProviderAggregate> page(int pageNum, int pageSize, String keyword,
                                              String vendor, Integer enabled) {
        LambdaQueryWrapper<ProviderPO> w = new LambdaQueryWrapper<ProviderPO>()
                .eq(enabled != null, ProviderPO::getEnabled, enabled)
                .eq(vendor != null && !vendor.isBlank(), ProviderPO::getVendor, vendor)
                .and(keyword != null && !keyword.isBlank(),
                        q -> q.like(ProviderPO::getName, keyword).or().like(ProviderPO::getCode, keyword))
                .orderByAsc(ProviderPO::getSort)
                .orderByDesc(ProviderPO::getCreatedAt);
        Page<ProviderPO> r = providerDao.selectPage(new Page<>(pageNum, pageSize), w);
        return PageResult.of(r.getRecords().stream().map(CONVERTOR::toDomain).toList(), r.getTotal());
    }

    @Override
    public List<ProviderAggregate> listEnabled() {
        return providerDao.selectList(new LambdaQueryWrapper<ProviderPO>()
                        .eq(ProviderPO::getEnabled, 1).orderByAsc(ProviderPO::getSort))
                .stream().map(CONVERTOR::toDomain).toList();
    }
}
