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
package vip.mate.crm.business.domain.adapter.repository;

import vip.mate.base.result.PageResult;
import vip.mate.crm.business.domain.model.aggregate.BusinessAggregate;
import vip.mate.crm.business.domain.model.valobj.BusinessQuery;
import vip.mate.crm.common.CrmScope;
import vip.mate.crm.query.dto.AdvancedQuery;

import java.util.List;

/**
 * 商机仓储接口(聚合含产品行)。
 *
 * @author mateaix
 */
public interface IBusinessRepository {

    void save(BusinessAggregate aggregate);

    void update(BusinessAggregate aggregate);

    BusinessAggregate findById(String id);

    void deleteById(String id);

    PageResult<BusinessAggregate> page(BusinessQuery query);

    /** 条件查询(动态高级筛选): 组间 OR / 组内 AND, 叠加数据权限范围。 */
    PageResult<BusinessAggregate> search(AdvancedQuery query, CrmScope scope);

    /** 某客户下的商机(客户 360 用)。 */
    List<BusinessAggregate> listByCustomerId(String customerId);

    /** 停滞在途商机(超 N 天未更新, AI 提醒用)。 */
    List<BusinessAggregate> findStale(int days, int limit);
}
