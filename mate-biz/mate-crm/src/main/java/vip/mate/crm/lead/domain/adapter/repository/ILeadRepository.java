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
package vip.mate.crm.lead.domain.adapter.repository;

import vip.mate.base.result.PageResult;
import vip.mate.crm.lead.domain.model.aggregate.LeadAggregate;
import vip.mate.crm.lead.domain.model.valobj.LeadQuery;

/**
 * 线索仓储接口(domain 定义, infrastructure 实现)。
 *
 * @author mateaix
 */
public interface ILeadRepository {

    void save(LeadAggregate aggregate);

    void update(LeadAggregate aggregate);

    LeadAggregate findById(String id);

    /** 手机号是否已被其他跟进中线索占用(转化防撞)。 */
    boolean existsFollowingByMobile(String mobile, String excludeId);

    void deleteById(String id);

    /** 分页查询(条件含数据权限范围)。 */
    PageResult<LeadAggregate> page(LeadQuery query);
}
