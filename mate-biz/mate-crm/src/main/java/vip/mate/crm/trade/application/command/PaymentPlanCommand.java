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
package vip.mate.crm.trade.application.command;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 回款计划期次新增/编辑命令。
 *
 * @author mateaix
 */
@Data
public class PaymentPlanCommand {

    private String contractId;
    private String contractNum;
    private String customerId;
    private Integer termNo;
    private BigDecimal money;
    private LocalDate checkingDate;
    private String returnType;
    private String remark;
}
