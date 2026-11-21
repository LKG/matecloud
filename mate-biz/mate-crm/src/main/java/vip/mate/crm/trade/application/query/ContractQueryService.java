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
package vip.mate.crm.trade.application.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vip.mate.base.result.PageResult;
import vip.mate.crm.common.CurrentUserResolver;
import vip.mate.crm.trade.domain.adapter.repository.IContractRepository;
import vip.mate.crm.trade.domain.model.aggregate.ContractAggregate;
import vip.mate.crm.trade.domain.model.entity.Contract;
import vip.mate.crm.trade.domain.model.entity.ContractProduct;
import vip.mate.crm.trade.domain.model.valobj.ContractQuery;

import java.util.List;

/** 合同读侧查询服务: 分页 + 明细(含产品行)。 */
@Service
@RequiredArgsConstructor
public class ContractQueryService {

    private final IContractRepository contractRepository;
    private final CurrentUserResolver currentUserResolver;

    public PageResult<Contract> page(String keyword, String status, String customerId,
                                     Boolean mineOnly, int pageNum, int pageSize) {
        String ownerId = Boolean.TRUE.equals(mineOnly) ? currentUserResolver.require().userId() : null;
        ContractQuery query = new ContractQuery(keyword, status, customerId, ownerId,
                currentUserResolver.scope(), pageNum, pageSize);
        PageResult<ContractAggregate> result = contractRepository.page(query);
        List<Contract> list = result.getList().stream().map(ContractAggregate::getContract).toList();
        return PageResult.of(list, result.getTotal());
    }

    public Contract findById(String id) {
        ContractAggregate agg = contractRepository.findById(id);
        return agg == null ? null : agg.getContract();
    }

    public ContractDetail detail(String id) {
        ContractAggregate agg = contractRepository.findById(id);
        if (agg == null) {
            return null;
        }
        return new ContractDetail(agg.getContract(), agg.getProducts());
    }

    /** 客户下合同列表(客户 360 用)。 */
    public List<Contract> listByCustomer(String customerId) {
        return contractRepository.listByCustomerId(customerId);
    }

    /** 合同明细读模型。 */
    public record ContractDetail(Contract contract, List<ContractProduct> products) {
    }
}
