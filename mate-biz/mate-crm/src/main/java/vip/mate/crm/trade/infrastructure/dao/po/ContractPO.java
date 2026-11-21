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
package vip.mate.crm.trade.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 合同持久化对象(mate_crm_contract)。lock_version 列保留但 OCC 未启用(与平台一致)。 */
@Data
@TableName("mate_crm_contract")
public class ContractPO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 租户列, 依赖 DB 默认值 '1' */
    private String tenantId;

    private String contractNum;
    private String contractName;
    private String customerId;
    private String customerName;
    private String businessId;
    private String contactName;
    private BigDecimal money;
    private BigDecimal receivedMoney;
    private LocalDate startTime;
    private LocalDate endTime;
    private LocalDate signTime;
    private String status;
    private String remark;
    private String attachment;
    private String ownerId;
    private String ownerName;
    private String deptId;
    private String creatorId;
    private String creatorName;

    @TableField(fill = FieldFill.INSERT)
    private Integer lockVersion;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
