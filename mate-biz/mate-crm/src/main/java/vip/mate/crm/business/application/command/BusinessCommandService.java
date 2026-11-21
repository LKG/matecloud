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
package vip.mate.crm.business.application.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.mate.base.exception.BizException;
import vip.mate.crm.business.application.command.BusinessCommand.ProductLine;
import vip.mate.crm.business.domain.adapter.repository.IBusinessRepository;
import vip.mate.crm.business.domain.model.aggregate.BusinessAggregate;
import vip.mate.crm.business.domain.model.entity.BusinessProduct;
import vip.mate.crm.common.CrmCurrentUser;
import vip.mate.crm.common.CurrentUserResolver;
import vip.mate.crm.types.exception.CrmErrorCode;

import java.util.List;

/**
 * 商机写侧应用服务: 创建/编辑/阶段推进/赢单/输单编排。
 *
 * @author mateaix
 */
@Service
@RequiredArgsConstructor
public class BusinessCommandService {

    private final IBusinessRepository businessRepository;
    private final CurrentUserResolver currentUserResolver;

    @Transactional
    public String create(BusinessCommand cmd) {
        CrmCurrentUser operator = currentUserResolver.require();
        BusinessAggregate agg = BusinessAggregate.create(
                cmd.getBusinessName(), cmd.getCustomerId(), cmd.getCustomerName(), cmd.getContactName(),
                cmd.getSource(), cmd.getDealProb(), cmd.getDealDate(), cmd.getNextStep(), cmd.getRemark(),
                toLines(cmd.getProducts()), operator);
        businessRepository.save(agg);
        return agg.getId();
    }

    @Transactional
    public void update(String id, BusinessCommand cmd) {
        BusinessAggregate agg = findOrThrow(id);
        agg.updateInfo(cmd.getBusinessName(), cmd.getContactName(), cmd.getSource(), cmd.getDealProb(),
                cmd.getDealDate(), cmd.getNextStep(), cmd.getRemark(), toLines(cmd.getProducts()));
        businessRepository.update(agg);
    }

    /** 逐级推进到下一活跃阶段。 */
    @Transactional
    public void advance(String id) {
        BusinessAggregate agg = findOrThrow(id);
        agg.advance();
        businessRepository.update(agg);
    }

    /** 推进到指定阶段(下一级/赢单/输单)。 */
    @Transactional
    public void advanceTo(String id, String stage) {
        BusinessAggregate agg = findOrThrow(id);
        agg.advanceTo(stage);
        businessRepository.update(agg);
    }

    @Transactional
    public void win(String id, String endRemark) {
        BusinessAggregate agg = findOrThrow(id);
        agg.win(endRemark);
        businessRepository.update(agg);
    }

    @Transactional
    public void lose(String id, String endRemark) {
        BusinessAggregate agg = findOrThrow(id);
        agg.lose(endRemark);
        businessRepository.update(agg);
    }

    @Transactional
    public void delete(String id) {
        findOrThrow(id);
        businessRepository.deleteById(id);
    }

    private static List<BusinessProduct> toLines(List<ProductLine> lines) {
        if (lines == null) {
            return List.of();
        }
        return lines.stream()
                .map(l -> BusinessProduct.of(l.getProductId(), l.getProductName(), l.getUnitPrice(), l.getQuantity()))
                .toList();
    }

    private BusinessAggregate findOrThrow(String id) {
        BusinessAggregate agg = businessRepository.findById(id);
        if (agg == null) {
            throw new BizException(CrmErrorCode.RECORD_NOT_FOUND);
        }
        return agg;
    }
}
