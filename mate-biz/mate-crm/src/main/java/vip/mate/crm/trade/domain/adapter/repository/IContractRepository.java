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
package vip.mate.crm.trade.domain.adapter.repository;

import vip.mate.base.result.PageResult;
import vip.mate.crm.common.CrmScope;
import vip.mate.crm.query.dto.AdvancedQuery;
import vip.mate.crm.trade.domain.model.aggregate.ContractAggregate;
import vip.mate.crm.trade.domain.model.entity.Contract;
import vip.mate.crm.trade.domain.model.valobj.ContractQuery;

import java.util.List;

/**
 * 合同仓储接口(聚合含产品行, domain 定义 infrastructure 实现)。
 *
 * @author mateaix
 */
public interface IContractRepository {

    void save(ContractAggregate aggregate);

    void update(ContractAggregate aggregate);

    ContractAggregate findById(String id);

    void deleteById(String id);

    PageResult<ContractAggregate> page(ContractQuery query);

    /** 条件查询(动态高级筛选)。 */
    PageResult<ContractAggregate> search(AdvancedQuery query, CrmScope scope);

    /** 合同编号查重(排除自身)。 */
    boolean existsByNum(String contractNum, String excludeId);

    /** 某客户下的合同(客户 360 用)。 */
    List<Contract> listByCustomerId(String customerId);

    /** 回款累加: 只更新主表 received_money(轻量, 不重载产品行)。 */
    void updateReceivedMoney(String id, java.math.BigDecimal receivedMoney);
}
