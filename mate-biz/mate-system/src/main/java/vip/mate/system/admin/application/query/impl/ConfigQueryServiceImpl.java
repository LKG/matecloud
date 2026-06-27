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
import vip.mate.system.admin.application.query.IConfigQueryService;
import vip.mate.system.admin.domain.config.adapter.repository.ConfigRepository;
import vip.mate.system.admin.domain.config.model.entity.Config;
import vip.mate.base.result.PageResult;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConfigQueryServiceImpl implements IConfigQueryService {

    private final ConfigRepository configRepository;

    @Override
    public ConfigVO findById(String id) { return toVO(configRepository.findById(id)); }

    @Override
    public ConfigVO findByKey(String configKey) { return toVO(configRepository.findByKey(configKey)); }

    @Override
    public List<ConfigVO> list() {
        return configRepository.list().stream().map(this::toVO).toList();
    }

    @Override
    public PageResult<ConfigVO> page(int pageNum, int pageSize, String keyword, Boolean builtIn) {
        PageResult<Config> raw = configRepository.page(pageNum, pageSize, keyword, builtIn);
        List<ConfigVO> rows = raw.getList().stream().map(this::toVO).toList();
        return PageResult.of(rows, raw.getTotal());
    }

    private ConfigVO toVO(Config c) {
        if (c == null) return null;
        return new ConfigVO(c.getId(), c.getConfigKey(), c.getConfigValue(),
                c.getConfigName(), c.getBuiltIn(), c.getRemark(), c.getCreatedAt());
    }
}
