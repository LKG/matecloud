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
package vip.mate.crm.customer.application.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vip.mate.base.result.PageResult;
import vip.mate.crm.common.CurrentUserResolver;
import vip.mate.crm.customer.domain.adapter.repository.IFollowUpRepository;
import vip.mate.crm.customer.domain.model.aggregate.FollowUpAggregate;
import vip.mate.crm.customer.domain.model.entity.FollowUp;

import java.util.List;

/** 跟进记录读侧查询服务。 */
@Service
@RequiredArgsConstructor
public class FollowUpQueryService {

    private final IFollowUpRepository followUpRepository;
    private final CurrentUserResolver currentUserResolver;

    public PageResult<FollowUp> page(String bizType, String bizId, int pageNum, int pageSize) {
        PageResult<FollowUpAggregate> result = followUpRepository.page(
                bizType, bizId, currentUserResolver.scope(), pageNum, pageSize);
        List<FollowUp> list = result.getList().stream().map(FollowUpAggregate::getFollowUp).toList();
        return PageResult.of(list, result.getTotal());
    }

    public FollowUp findById(String id) {
        FollowUpAggregate agg = followUpRepository.findById(id);
        return agg == null ? null : agg.getFollowUp();
    }
}
