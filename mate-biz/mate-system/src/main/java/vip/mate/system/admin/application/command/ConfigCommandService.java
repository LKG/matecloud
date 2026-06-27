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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.mate.system.admin.domain.config.adapter.repository.ConfigRepository;
import vip.mate.system.admin.domain.config.model.entity.Config;
import vip.mate.system.admin.types.exception.AdminErrorCode;
import vip.mate.base.exception.BizException;

@Service
@RequiredArgsConstructor
public class ConfigCommandService {

    private final ConfigRepository configRepository;

    @Transactional
    public String createConfig(String configKey, String configValue, String configName, String remark) {
        if (configRepository.existsByKey(configKey)) {
            throw BizException.of(AdminErrorCode.DUPLICATE_CONFIG_KEY);
        }
        Config config = Config.create(configKey, configValue, configName, remark);
        configRepository.save(config);
        return config.getId();
    }

    @Transactional
    public void updateConfig(String id, String configValue, String remark) {
        Config config = findOrThrow(id);
        config.setConfigValue(configValue);
        config.setRemark(remark);
        configRepository.update(config);
    }

    @Transactional
    public void deleteConfig(String id) {
        Config config = findOrThrow(id);
        if (Boolean.TRUE.equals(config.getBuiltIn())) {
            throw BizException.of(AdminErrorCode.CANNOT_DELETE_BUILTIN_CONFIG);
        }
        configRepository.deleteById(id);
    }

    private Config findOrThrow(String id) {
        Config config = configRepository.findById(id);
        if (config == null) throw BizException.of(AdminErrorCode.CONFIG_NOT_EXIST);
        return config;
    }
}
