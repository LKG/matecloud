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
package vip.mate.crm.customer.application.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.mate.crm.customer.domain.adapter.repository.IPoolConfigRepository;
import vip.mate.crm.customer.domain.model.entity.PoolConfig;

/**
 * 公海规则配置写侧服务(单行 upsert)。
 *
 * @author mateaix
 */
@Service
@RequiredArgsConstructor
public class PoolConfigCommandService {

    private final IPoolConfigRepository poolConfigRepository;

    @Transactional
    public void save(PoolConfigCommand cmd) {
        PoolConfig current = poolConfigRepository.load();
        PoolConfig next = PoolConfig.builder()
                .id(current.getId())
                .enabled(cmd.getEnabled() == null ? current.getEnabled() : cmd.getEnabled())
                .recycleDays(cmd.getRecycleDays() == null ? current.getRecycleDays() : cmd.getRecycleDays())
                .maxHold(cmd.getMaxHold() == null ? current.getMaxHold() : cmd.getMaxHold())
                .remindDays(cmd.getRemindDays() == null ? current.getRemindDays() : cmd.getRemindDays())
                .build();
        if (next.getId() == null) {
            poolConfigRepository.save(next);
        } else {
            poolConfigRepository.update(next);
        }
    }
}
