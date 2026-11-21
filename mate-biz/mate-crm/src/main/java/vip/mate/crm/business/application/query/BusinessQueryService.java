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
package vip.mate.crm.business.application.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vip.mate.base.exception.BizException;
import vip.mate.base.result.PageResult;
import vip.mate.crm.business.domain.adapter.repository.IBusinessRepository;
import vip.mate.crm.business.domain.model.aggregate.BusinessAggregate;
import vip.mate.crm.business.domain.model.entity.Business;
import vip.mate.crm.business.domain.model.entity.BusinessProduct;
import vip.mate.crm.business.domain.model.valobj.BusinessQuery;
import vip.mate.crm.common.CurrentUserResolver;
import vip.mate.crm.types.exception.CrmErrorCode;

import java.util.List;

/** 商机读侧查询服务: 分页 + 漏斗视图 + 明细(含产品行)。 */
@Service
@RequiredArgsConstructor
public class BusinessQueryService {

    private final IBusinessRepository businessRepository;
    private final CurrentUserResolver currentUserResolver;

    public PageResult<Business> page(String keyword, String stage, String customerId, Integer statusFlag,
                                     Boolean mineOnly, int pageNum, int pageSize) {
        String ownerId = Boolean.TRUE.equals(mineOnly) ? currentUserResolver.require().userId() : null;
        BusinessQuery query = new BusinessQuery(keyword, stage, customerId, statusFlag, ownerId,
                currentUserResolver.scope(), pageNum, pageSize);
        PageResult<BusinessAggregate> result = businessRepository.page(query);
        List<Business> list = result.getList().stream().map(BusinessAggregate::getBusiness).toList();
        return PageResult.of(list, result.getTotal());
    }

    public Business findById(String id) {
        BusinessAggregate agg = businessRepository.findById(id);
        return agg == null ? null : agg.getBusiness();
    }

    public BusinessDetail detail(String id) {
        BusinessAggregate agg = businessRepository.findById(id);
        if (agg == null) {
            throw new BizException(CrmErrorCode.RECORD_NOT_FOUND);
        }
        return new BusinessDetail(agg.getBusiness(), agg.getProducts());
    }

    /** 客户下商机列表(360 用)。 */
    public List<Business> listByCustomer(String customerId) {
        return businessRepository.listByCustomerId(customerId).stream()
                .map(BusinessAggregate::getBusiness).toList();
    }

    /** 商机明细读模型。 */
    public record BusinessDetail(Business business, List<BusinessProduct> products) {
    }
}
