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
package vip.mate.crm.customer.infrastructure.adapter.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vip.mate.crm.customer.domain.adapter.repository.IPoolConfigRepository;
import vip.mate.crm.customer.domain.model.entity.PoolConfig;
import vip.mate.crm.customer.infrastructure.dao.PoolConfigDao;
import vip.mate.crm.customer.infrastructure.dao.po.PoolConfigPO;

/** 公海规则配置仓储实现(取首行, 缺省回退内置默认)。 */
@Repository
@RequiredArgsConstructor
public class PoolConfigRepositoryImpl implements IPoolConfigRepository {

    private final PoolConfigDao poolConfigDao;

    @Override
    public PoolConfig load() {
        LambdaQueryWrapper<PoolConfigPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(PoolConfigPO::getId).last("limit 1");
        PoolConfigPO po = poolConfigDao.selectOne(wrapper);
        if (po == null) {
            return PoolConfig.builder()
                    .enabled(1).recycleDays(30).maxHold(200).remindDays(7).build();
        }
        return PoolConfig.builder()
                .id(po.getId())
                .enabled(po.getEnabled())
                .recycleDays(po.getRecycleDays())
                .maxHold(po.getMaxHold())
                .remindDays(po.getRemindDays())
                .build();
    }

    @Override
    public void save(PoolConfig config) {
        poolConfigDao.insert(toPO(config));
    }

    @Override
    public void update(PoolConfig config) {
        poolConfigDao.updateById(toPO(config));
    }

    private PoolConfigPO toPO(PoolConfig config) {
        PoolConfigPO po = new PoolConfigPO();
        po.setId(config.getId());
        po.setEnabled(config.getEnabled());
        po.setRecycleDays(config.getRecycleDays());
        po.setMaxHold(config.getMaxHold());
        po.setRemindDays(config.getRemindDays());
        return po;
    }
}
