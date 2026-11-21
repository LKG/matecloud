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
import vip.mate.crm.customer.domain.model.aggregate.CustomerAggregate;
import vip.mate.crm.customer.domain.model.valobj.CustomerQuery;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 客户仓储接口(domain 定义, infrastructure 实现)。
 *
 * @author mateaix
 */
public interface ICustomerRepository {

    void save(CustomerAggregate aggregate);

    void update(CustomerAggregate aggregate);

    CustomerAggregate findById(String id);

    void deleteById(String id);

    PageResult<CustomerAggregate> page(CustomerQuery query);

    /** 名称查重(排除自身); 命中返回 true。 */
    boolean existsByName(String customerName, String excludeId);

    /** 手机号查重(排除自身)。 */
    boolean existsByPhone(String phone, String excludeId);

    /** 某负责人当前私海客户数(领取/报备上限校验)。 */
    long countPrivateByOwner(String ownerId);

    /** 公海回收 job: 私海且最近跟进早于 cutoff(或从未跟进且创建早于 cutoff)的客户。 */
    List<CustomerAggregate> findRecyclable(LocalDateTime cutoff, int limit);
}
