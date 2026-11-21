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
package vip.mate.crm.customer.domain.model.entity;

import lombok.Builder;
import lombok.Data;

/**
 * 公海规则配置(单行): 自动回收开关 + 回收天数 + 私海持有上限 + 提前提醒天数。
 *
 * @author mateaix
 */
@Data
@Builder
public class PoolConfig {

    private String id;
    /** 1=启用自动回收 */
    private Integer enabled;
    /** 私海客户 N 天无跟进自动回收公海 */
    private Integer recycleDays;
    /** 单人私海客户上限(0=不限) */
    private Integer maxHold;
    /** 回收前 N 天提醒 */
    private Integer remindDays;

    public boolean recycleEnabled() {
        return enabled != null && enabled == 1 && recycleDays != null && recycleDays > 0;
    }
}
