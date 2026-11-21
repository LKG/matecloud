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
package vip.mate.crm.aftersale.application.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vip.mate.base.result.PageResult;
import vip.mate.crm.aftersale.domain.adapter.repository.IAfterSaleRepository;
import vip.mate.crm.aftersale.domain.model.aggregate.AfterSaleAggregate;
import vip.mate.crm.aftersale.domain.model.entity.AfterSale;
import vip.mate.crm.aftersale.domain.model.entity.AfterSaleLog;
import vip.mate.crm.common.CurrentUserResolver;

import java.util.List;

/** 售后工单读侧查询服务。 */
@Service
@RequiredArgsConstructor
public class AfterSaleQueryService {

    private final IAfterSaleRepository afterSaleRepository;
    private final CurrentUserResolver currentUserResolver;

    public PageResult<AfterSale> page(String keyword, String status, String type, String customerId,
                                      Boolean mineOnly, int pageNum, int pageSize) {
        String ownerId = Boolean.TRUE.equals(mineOnly) ? currentUserResolver.require().userId() : null;
        return afterSaleRepository.page(keyword, status, type, customerId, ownerId,
                currentUserResolver.scope(), pageNum, pageSize);
    }

    public AfterSale findById(String id) {
        AfterSaleAggregate agg = afterSaleRepository.findById(id);
        return agg == null ? null : agg.getAfterSale();
    }

    public AfterSaleDetail detail(String id) {
        AfterSaleAggregate agg = afterSaleRepository.findById(id);
        if (agg == null) {
            return null;
        }
        return new AfterSaleDetail(agg.getAfterSale(), agg.getLogs());
    }

    /** 工单明细读模型(含处理时间线)。 */
    public record AfterSaleDetail(AfterSale afterSale, List<AfterSaleLog> logs) {
    }
}
