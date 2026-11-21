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
package vip.mate.crm.customer.application.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.mate.base.exception.BizException;
import vip.mate.crm.common.CrmCurrentUser;
import vip.mate.crm.common.CurrentUserResolver;
import vip.mate.crm.customer.domain.adapter.repository.IFollowUpRepository;
import vip.mate.crm.customer.domain.model.aggregate.FollowUpAggregate;
import vip.mate.crm.types.exception.CrmErrorCode;

import java.time.LocalDateTime;

/**
 * 跟进写侧应用服务: 落跟进记录并回写客户最近跟进时间(公海回收计时)。
 *
 * @author mateaix
 */
@Service
@RequiredArgsConstructor
public class FollowUpCommandService {

    private final IFollowUpRepository followUpRepository;
    private final CustomerCommandService customerCommandService;
    private final CurrentUserResolver currentUserResolver;

    @Transactional
    public String save(FollowUpCommand cmd) {
        CrmCurrentUser operator = currentUserResolver.require();
        LocalDateTime now = LocalDateTime.now();
        FollowUpAggregate agg = FollowUpAggregate.create(
                cmd.getBizType(), cmd.getBizId(), cmd.getBizName(), cmd.getCustomerId(), cmd.getContent(),
                cmd.getFollowType(), cmd.getNextContent(), cmd.getNextTime(), operator);
        followUpRepository.save(agg);
        // 客户维度跟进: 回写最近跟进时间, 影响公海回收判定。
        String customerId = agg.getFollowUp().getCustomerId();
        if (customerId != null && !customerId.isBlank()) {
            customerCommandService.touchFollow(customerId, now, cmd.getNextTime());
        }
        return agg.getId();
    }

    @Transactional
    public void delete(String id) {
        FollowUpAggregate agg = followUpRepository.findById(id);
        if (agg == null) {
            throw new BizException(CrmErrorCode.RECORD_NOT_FOUND);
        }
        followUpRepository.deleteById(id);
    }
}
