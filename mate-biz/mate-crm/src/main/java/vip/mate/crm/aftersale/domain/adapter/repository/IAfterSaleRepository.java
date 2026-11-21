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
package vip.mate.crm.aftersale.domain.adapter.repository;

import vip.mate.base.result.PageResult;
import vip.mate.crm.aftersale.domain.model.aggregate.AfterSaleAggregate;
import vip.mate.crm.aftersale.domain.model.entity.AfterSale;
import vip.mate.crm.common.CrmScope;
import vip.mate.crm.query.dto.AdvancedQuery;

/**
 * 售后工单仓储接口(聚合含处理日志)。
 *
 * @author mateaix
 */
public interface IAfterSaleRepository {

    void save(AfterSaleAggregate aggregate);

    void update(AfterSaleAggregate aggregate);

    AfterSaleAggregate findById(String id);

    void deleteById(String id);

    PageResult<AfterSale> page(String keyword, String status, String type, String customerId,
                               String ownerId, CrmScope scope, int pageNum, int pageSize);

    /** 条件查询(动态高级筛选)。 */
    PageResult<AfterSale> search(AdvancedQuery query, CrmScope scope);
}
