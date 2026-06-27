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
import vip.mate.system.admin.domain.config.adapter.repository.ConfigRepository;
import vip.mate.system.admin.domain.config.model.entity.Config;
import vip.mate.system.admin.infrastructure.dao.ConfigDao;
import vip.mate.system.admin.infrastructure.dao.po.ConfigPO;
import vip.mate.base.result.PageResult;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ConfigRepositoryImpl implements ConfigRepository {

    private final ConfigDao configDao;

    @Override
    public void save(Config config) { configDao.insert(toPO(config)); }

    @Override
    public void update(Config config) { configDao.updateById(toPO(config)); }

    @Override
    public void deleteById(String id) { configDao.deleteById(id); }

    @Override
    public Config findById(String id) {
        ConfigPO po = configDao.selectById(id);
        return po != null ? toEntity(po) : null;
    }

    @Override
    public Config findByKey(String configKey) {
        ConfigPO po = configDao.selectByKey(configKey);
        return po != null ? toEntity(po) : null;
    }

    @Override
    public boolean existsByKey(String configKey) { return configDao.existsByKey(configKey); }

    @Override
    public List<Config> list() {
        return configDao.selectList(null).stream().map(this::toEntity).toList();
    }

    @Override
    public PageResult<Config> page(int pageNum, int pageSize, String keyword, Boolean builtIn) {
        LambdaQueryWrapper<ConfigPO> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(ConfigPO::getConfigKey, keyword)
                    .or().like(ConfigPO::getConfigName, keyword));
        }
        if (builtIn != null) {
            wrapper.eq(ConfigPO::getBuiltIn, builtIn);
        }
        wrapper.orderByDesc(ConfigPO::getCreatedAt);

        Page<ConfigPO> page = configDao.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<Config> rows = page.getRecords().stream().map(this::toEntity).toList();
        return PageResult.of(rows, page.getTotal());
    }

    private ConfigPO toPO(Config c) {
        ConfigPO po = new ConfigPO();
        po.setId(c.getId());
        po.setConfigKey(c.getConfigKey());
        po.setConfigValue(c.getConfigValue());
        po.setConfigName(c.getConfigName());
        po.setBuiltIn(c.getBuiltIn());
        po.setRemark(c.getRemark());
        return po;
    }

    private Config toEntity(ConfigPO po) {
        return Config.builder()
                .id(po.getId())
                .configKey(po.getConfigKey())
                .configValue(po.getConfigValue())
                .configName(po.getConfigName())
                .builtIn(po.getBuiltIn())
                .remark(po.getRemark())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
}
