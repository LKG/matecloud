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
package vip.mate.crm.aftersale.application.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.mate.base.exception.BizException;
import vip.mate.crm.aftersale.domain.adapter.repository.IAfterSaleRepository;
import vip.mate.crm.aftersale.domain.model.aggregate.AfterSaleAggregate;
import vip.mate.crm.common.CrmCurrentUser;
import vip.mate.crm.common.CurrentUserResolver;
import vip.mate.crm.types.exception.CrmErrorCode;

/**
 * 售后工单写侧应用服务: 创建/受理/处理/完成/关闭/评价编排。
 *
 * @author mateaix
 */
@Service
@RequiredArgsConstructor
public class AfterSaleCommandService {

    private final IAfterSaleRepository afterSaleRepository;
    private final CurrentUserResolver currentUserResolver;

    @Transactional
    public String create(AfterSaleCommand cmd) {
        CrmCurrentUser operator = currentUserResolver.require();
        AfterSaleAggregate agg = AfterSaleAggregate.create(
                cmd.getTitle(), cmd.getCustomerId(), cmd.getCustomerName(), cmd.getContractId(),
                cmd.getType(), cmd.getPriority(), cmd.getDescription(), operator);
        afterSaleRepository.save(agg);
        return agg.getId();
    }

    @Transactional
    public void accept(String id) {
        AfterSaleAggregate agg = findOrThrow(id);
        agg.accept(currentUserResolver.require());
        afterSaleRepository.update(agg);
    }

    @Transactional
    public void process(String id, String content) {
        AfterSaleAggregate agg = findOrThrow(id);
        agg.process(content, currentUserResolver.require());
        afterSaleRepository.update(agg);
    }

    @Transactional
    public void resolve(String id, String content) {
        AfterSaleAggregate agg = findOrThrow(id);
        agg.resolve(content, currentUserResolver.require());
        afterSaleRepository.update(agg);
    }

    @Transactional
    public void close(String id, String content) {
        AfterSaleAggregate agg = findOrThrow(id);
        agg.close(content, currentUserResolver.require());
        afterSaleRepository.update(agg);
    }

    @Transactional
    public void rate(String id, Integer rating, String remark) {
        AfterSaleAggregate agg = findOrThrow(id);
        agg.rate(rating, remark, currentUserResolver.require());
        afterSaleRepository.update(agg);
    }

    @Transactional
    public void delete(String id) {
        findOrThrow(id);
        afterSaleRepository.deleteById(id);
    }

    private AfterSaleAggregate findOrThrow(String id) {
        AfterSaleAggregate agg = afterSaleRepository.findById(id);
        if (agg == null) {
            throw new BizException(CrmErrorCode.RECORD_NOT_FOUND);
        }
        return agg;
    }
}
