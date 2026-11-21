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
import vip.mate.crm.customer.domain.adapter.repository.ICustomerRepository;
import vip.mate.crm.customer.domain.adapter.repository.IPoolConfigRepository;
import vip.mate.crm.customer.domain.model.aggregate.CustomerAggregate;
import vip.mate.crm.customer.domain.model.entity.PoolConfig;
import vip.mate.crm.types.exception.CrmErrorCode;

/**
 * 客户写侧应用服务: 报备查重、公海/私海流转、上限校验编排。
 *
 * @author mateaix
 */
@Service
@RequiredArgsConstructor
public class CustomerCommandService {

    private final ICustomerRepository customerRepository;
    private final IPoolConfigRepository poolConfigRepository;
    private final CurrentUserResolver currentUserResolver;

    @Transactional
    public String create(CustomerCommand cmd) {
        CrmCurrentUser operator = currentUserResolver.require();
        assertUnique(cmd.getCustomerName(), cmd.getPhone(), null);
        PoolConfig config = poolConfigRepository.load();
        int hold = (int) customerRepository.countPrivateByOwner(operator.userId());
        CustomerAggregate agg = CustomerAggregate.create(
                cmd.getCustomerName(), cmd.getIndustry(), cmd.getLevel(), cmd.getSource(), cmd.getStatus(),
                cmd.getPhone(), cmd.getAddress(), cmd.getWebsite(), cmd.getRemark(),
                cmd.isToPublicPool(), operator, hold, config.getMaxHold() == null ? 0 : config.getMaxHold());
        customerRepository.save(agg);
        return agg.getId();
    }

    @Transactional
    public void update(String id, CustomerCommand cmd) {
        assertUnique(cmd.getCustomerName(), cmd.getPhone(), id);
        CustomerAggregate agg = findOrThrow(id);
        agg.updateInfo(cmd.getCustomerName(), cmd.getIndustry(), cmd.getLevel(), cmd.getSource(),
                cmd.getStatus(), cmd.getPhone(), cmd.getAddress(), cmd.getWebsite(), cmd.getRemark());
        customerRepository.update(agg);
    }

    /** 公海领取。 */
    @Transactional
    public void claim(String id) {
        CrmCurrentUser operator = currentUserResolver.require();
        CustomerAggregate agg = findOrThrow(id);
        PoolConfig config = poolConfigRepository.load();
        int hold = (int) customerRepository.countPrivateByOwner(operator.userId());
        agg.claim(operator, hold, config.getMaxHold() == null ? 0 : config.getMaxHold());
        customerRepository.update(agg);
    }

    /** 释放回公海。 */
    @Transactional
    public void release(String id, String reason) {
        CustomerAggregate agg = findOrThrow(id);
        agg.release(reason);
        customerRepository.update(agg);
    }

    /** 转移负责人。 */
    @Transactional
    public void transfer(String id, String ownerId, String ownerName, String deptId) {
        CustomerAggregate agg = findOrThrow(id);
        agg.transfer(ownerId, ownerName, deptId);
        customerRepository.update(agg);
    }

    @Transactional
    public void delete(String id) {
        findOrThrow(id);
        customerRepository.deleteById(id);
    }

    /** 供跟进/线索转化等内部编排刷新最近跟进时间。 */
    @Transactional
    public void touchFollow(String id, java.time.LocalDateTime followTime, java.time.LocalDateTime nextTime) {
        CustomerAggregate agg = customerRepository.findById(id);
        if (agg == null) {
            return;
        }
        agg.touchFollow(followTime, nextTime);
        customerRepository.update(agg);
    }

    private void assertUnique(String name, String phone, String excludeId) {
        if (customerRepository.existsByName(name, excludeId)) {
            throw new BizException(CrmErrorCode.CUSTOMER_NAME_DUPLICATE);
        }
        if (phone != null && !phone.isBlank() && customerRepository.existsByPhone(phone, excludeId)) {
            throw new BizException(CrmErrorCode.CUSTOMER_PHONE_DUPLICATE);
        }
    }

    private CustomerAggregate findOrThrow(String id) {
        CustomerAggregate agg = customerRepository.findById(id);
        if (agg == null) {
            throw new BizException(CrmErrorCode.RECORD_NOT_FOUND);
        }
        return agg;
    }
}
