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
package vip.mate.crm.customer.domain.model.aggregate;

import lombok.Builder;
import lombok.Getter;
import vip.mate.base.exception.BizException;
import vip.mate.base.response.ResponseCode;
import vip.mate.crm.common.CrmCurrentUser;
import vip.mate.crm.customer.domain.model.entity.Customer;
import vip.mate.crm.customer.types.enums.CustomerStatus;
import vip.mate.crm.types.exception.CrmErrorCode;
import vip.mate.starter.distribute.util.SnowflakeUtil;

import java.time.LocalDateTime;

/**
 * 客户聚合根: 公海(pool_status=1, 无负责人) / 私海(pool_status=0, 有负责人) 流转。
 *
 * <p>不变式: 领取必须来自公海; 释放/转移必须来自私海; 私海客户数受 maxHold 约束
 * (由应用层传入当前持有数)。负责人视角的数据权限由查询侧 wrapper 保证。
 *
 * @author mateaix
 */
@Getter
@Builder
public class CustomerAggregate {

    public static final int POOL_PUBLIC = 1;
    public static final int POOL_PRIVATE = 0;

    private final Customer customer;

    public static CustomerAggregate create(String customerName, String industry, String level, String source,
                                           String status, String phone, String address, String website, String remark,
                                           boolean toPublicPool, CrmCurrentUser operator, int currentHold, int maxHold) {
        requireText(customerName, "客户名称不能为空");
        // 报备: 进私海才占负责人并受上限约束; 直接进公海则无负责人。
        if (!toPublicPool) {
            assertHoldLimit(currentHold, maxHold);
        }
        Customer customer = Customer.builder()
                .customerName(customerName)
                .industry(industry)
                .level(level == null || level.isBlank() ? "C" : level)
                .source(source == null || source.isBlank() ? "OTHER" : source)
                .status(status == null || status.isBlank() ? CustomerStatus.INITIAL.name() : status)
                .phone(phone)
                .address(address)
                .website(website)
                .remark(remark)
                .poolStatus(toPublicPool ? POOL_PUBLIC : POOL_PRIVATE)
                .claimTime(toPublicPool ? null : LocalDateTime.now())
                .toPoolTime(toPublicPool ? LocalDateTime.now() : null)
                .creatorId(operator.userId())
                .creatorName(operator.displayName())
                .build();
        customer.setId(SnowflakeUtil.newSnowflakeId());
        if (!toPublicPool) {
            customer.setOwnerId(operator.userId());
            customer.setOwnerName(operator.displayName());
            customer.setDeptId(operator.deptId());
        }
        return CustomerAggregate.builder().customer(customer).build();
    }

    public static CustomerAggregate of(Customer customer) {
        if (customer == null) {
            throw new BizException(CrmErrorCode.RECORD_NOT_FOUND);
        }
        return CustomerAggregate.builder().customer(customer).build();
    }

    /** 编辑基本信息(仅私海在库客户由负责人/权限侧控制, 此处只做终态无关校验)。 */
    public void updateInfo(String customerName, String industry, String level, String source,
                           String status, String phone, String address, String website, String remark) {
        requireText(customerName, "客户名称不能为空");
        customer.setCustomerName(customerName);
        customer.setIndustry(industry);
        if (level != null && !level.isBlank()) {
            customer.setLevel(level);
        }
        if (source != null && !source.isBlank()) {
            customer.setSource(source);
        }
        if (status != null && !status.isBlank()) {
            customer.setStatus(status);
        }
        customer.setPhone(phone);
        customer.setAddress(address);
        customer.setWebsite(website);
        customer.setRemark(remark);
    }

    /** 公海领取: 挂到操作人名下, 受上限约束。 */
    public void claim(CrmCurrentUser operator, int currentHold, int maxHold) {
        if (customer.getPoolStatus() != null && customer.getPoolStatus() == POOL_PRIVATE) {
            throw new BizException(CrmErrorCode.CUSTOMER_NOT_IN_POOL.getCode(), "客户已在私海，无法重复领取");
        }
        assertHoldLimit(currentHold, maxHold);
        customer.setPoolStatus(POOL_PRIVATE);
        customer.setOwnerId(operator.userId());
        customer.setOwnerName(operator.displayName());
        customer.setDeptId(operator.deptId());
        customer.setClaimTime(LocalDateTime.now());
        customer.setToPoolTime(null);
        customer.setToPoolReason(null);
    }

    /** 释放回公海(清空负责人)。 */
    public void release(String reason) {
        if (customer.getPoolStatus() != null && customer.getPoolStatus() == POOL_PUBLIC) {
            throw new BizException(CrmErrorCode.CUSTOMER_IN_POOL);
        }
        customer.setPoolStatus(POOL_PUBLIC);
        customer.setOwnerId(null);
        customer.setOwnerName(null);
        customer.setDeptId(null);
        customer.setClaimTime(null);
        customer.setToPoolTime(LocalDateTime.now());
        customer.setToPoolReason(reason == null || reason.isBlank() ? "手动释放" : reason);
    }

    /** 转移负责人(私海 → 他人私海)。 */
    public void transfer(String ownerId, String ownerName, String deptId) {
        if (customer.getPoolStatus() != null && customer.getPoolStatus() == POOL_PUBLIC) {
            throw new BizException(CrmErrorCode.CUSTOMER_NOT_IN_POOL.getCode(), "公海客户请先领取再转移");
        }
        requireText(ownerId, "接收人不能为空");
        customer.setOwnerId(ownerId);
        customer.setOwnerName(ownerName);
        customer.setDeptId(deptId);
        customer.setClaimTime(LocalDateTime.now());
    }

    /** 跟进后刷新最近跟进时间(公海回收计时起点)。 */
    public void touchFollow(LocalDateTime followTime, LocalDateTime nextTime) {
        customer.setRecentFollowTime(followTime == null ? LocalDateTime.now() : followTime);
        if (nextTime != null) {
            customer.setNextTime(nextTime);
        }
    }

    public String getId() {
        return customer.getId();
    }

    private static void assertHoldLimit(int currentHold, int maxHold) {
        if (maxHold > 0 && currentHold >= maxHold) {
            throw new BizException(CrmErrorCode.CUSTOMER_HOLD_LIMIT);
        }
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BizException(ResponseCode.PARAM_VALID_ERROR.getCode(), message);
        }
    }
}
