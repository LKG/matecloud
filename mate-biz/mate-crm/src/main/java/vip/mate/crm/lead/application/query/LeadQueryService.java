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
package vip.mate.crm.lead.application.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vip.mate.base.result.PageResult;
import vip.mate.crm.common.CurrentUserResolver;
import vip.mate.crm.lead.domain.adapter.repository.ILeadRepository;
import vip.mate.crm.lead.domain.model.aggregate.LeadAggregate;
import vip.mate.crm.lead.domain.model.entity.Lead;
import vip.mate.crm.lead.domain.model.valobj.LeadQuery;

import java.util.List;

/** 线索读侧实现: 组装数据权限 + mine 过滤后委托仓储。 */
@Service
@RequiredArgsConstructor
public class LeadQueryService implements ILeadQueryService {

    private final ILeadRepository leadRepository;
    private final CurrentUserResolver currentUserResolver;

    @Override
    public PageResult<Lead> page(String keyword, String status, String source, Boolean mineOnly,
                                 int pageNum, int pageSize) {
        String ownerId = Boolean.TRUE.equals(mineOnly) ? currentUserResolver.require().userId() : null;
        LeadQuery query = new LeadQuery(keyword, status, source, ownerId,
                currentUserResolver.scope(), pageNum, pageSize);
        PageResult<LeadAggregate> result = leadRepository.page(query);
        List<Lead> list = result.getList().stream().map(LeadAggregate::getLead).toList();
        return PageResult.of(list, result.getTotal());
    }

    @Override
    public Lead findById(String id) {
        LeadAggregate agg = leadRepository.findById(id);
        return agg == null ? null : agg.getLead();
    }
}
