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
package vip.mate.crm.customer.domain.adapter.repository;

import vip.mate.base.result.PageResult;
import vip.mate.crm.common.CrmScope;
import vip.mate.crm.customer.domain.model.aggregate.FollowUpAggregate;

import java.util.List;

/**
 * 跟进记录仓储接口。
 *
 * @author mateaix
 */
public interface IFollowUpRepository {

    void save(FollowUpAggregate aggregate);

    FollowUpAggregate findById(String id);

    void deleteById(String id);

    PageResult<FollowUpAggregate> page(String bizType, String bizId, CrmScope scope, int pageNum, int pageSize);

    /** 某业务对象的跟进时间线(倒序)。 */
    List<FollowUpAggregate> listByBiz(String bizType, String bizId);

    /** 某客户的全部跟进(360 视图聚合用)。 */
    List<FollowUpAggregate> listByCustomerId(String customerId);
}
