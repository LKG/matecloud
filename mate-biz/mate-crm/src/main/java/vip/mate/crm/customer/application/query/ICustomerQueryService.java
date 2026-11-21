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

import vip.mate.base.result.PageResult;
import vip.mate.crm.customer.domain.model.entity.Contact;
import vip.mate.crm.customer.domain.model.entity.Customer;
import vip.mate.crm.customer.domain.model.entity.FollowUp;
import vip.mate.crm.query.dto.AdvancedQuery;

import java.util.List;

/**
 * 客户读侧查询服务。
 *
 * @author mateaix
 */
public interface ICustomerQueryService {

    PageResult<Customer> page(String keyword, String industry, String level, String status,
                              Integer poolStatus, Boolean mineOnly, int pageNum, int pageSize);

    /** 条件查询(动态高级筛选), 叠加当前用户数据权限。 */
    PageResult<Customer> search(AdvancedQuery query);

    Customer findById(String id);

    /** 客户 360: 基本信息 + 联系人 + 跟进时间线(商机/合同/AI 摘要由各自上下文补充)。 */
    CustomerDetail detail(String id);

    /** 客户 360 读模型。 */
    record CustomerDetail(Customer customer, List<Contact> contacts, List<FollowUp> followUps) {
    }
}
