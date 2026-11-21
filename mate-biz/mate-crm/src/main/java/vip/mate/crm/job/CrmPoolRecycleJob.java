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
package vip.mate.crm.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vip.mate.crm.customer.domain.adapter.repository.ICustomerRepository;
import vip.mate.crm.customer.domain.adapter.repository.IPoolConfigRepository;
import vip.mate.crm.customer.domain.model.aggregate.CustomerAggregate;
import vip.mate.crm.customer.domain.model.entity.PoolConfig;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公海自动回收任务: 私海客户超过 N 天无跟进自动回落到公海(读 {@code mate_crm_pool_config})。
 *
 * <p>cron 走配置项, 默认每天 03:00; 配置未启用自动回收时直接跳过。
 *
 * @author mateaix
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrmPoolRecycleJob {

    private static final int BATCH_LIMIT = 500;

    private final ICustomerRepository customerRepository;
    private final IPoolConfigRepository poolConfigRepository;

    @Scheduled(cron = "${mate.crm.job.pool-recycle.cron:0 0 3 * * ?}")
    public void recycle() {
        PoolConfig config = poolConfigRepository.load();
        if (!config.recycleEnabled()) {
            log.debug("[crm-job] pool recycle disabled, skip");
            return;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusDays(config.getRecycleDays());
        List<CustomerAggregate> recyclable = customerRepository.findRecyclable(cutoff, BATCH_LIMIT);
        if (recyclable.isEmpty()) {
            log.info("[crm-job] pool recycle: nothing to recycle (cutoff={})", cutoff);
            return;
        }
        for (CustomerAggregate agg : recyclable) {
            agg.release("超过 " + config.getRecycleDays() + " 天无跟进, 系统自动回收");
            customerRepository.update(agg);
        }
        log.info("[crm-job] pool recycle: released {} customer(s) to public pool (cutoff={})",
                recyclable.size(), cutoff);
    }
}
