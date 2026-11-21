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
package vip.mate.crm.types.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import vip.mate.base.exception.ErrorCode;

/**
 * CRM business error codes.
 *
 * <p>Code scheme per AGENTS.md: {MODULE}{TYPE}{SEQ} — module {@code CRM},
 * type {@code B} = business rule violation.
 *
 * @author mateaix
 */
@Getter
@AllArgsConstructor
public enum CrmErrorCode implements ErrorCode {

    // ---- 通用 ----
    RECORD_NOT_FOUND("CRMB001", "记录不存在"),
    NO_PERMISSION("CRMB002", "无权操作该记录"),

    // ---- 线索 ----
    LEAD_ALREADY_CONVERTED("CRMB010", "线索已转化，不可重复操作"),
    LEAD_INVALID("CRMB011", "线索已废弃，不可再操作"),

    // ---- 客户 ----
    CUSTOMER_NAME_DUPLICATE("CRMB020", "客户名称已存在（报备查重拦截）"),
    CUSTOMER_PHONE_DUPLICATE("CRMB021", "该手机号已被其他客户报备"),
    CUSTOMER_NOT_IN_POOL("CRMB022", "客户不在公海，无法领取"),
    CUSTOMER_IN_POOL("CRMB023", "客户已在公海，无需此操作"),
    CUSTOMER_OWNER_CONFLICT("CRMB024", "客户已有负责人，请先转移或释放"),
    CUSTOMER_HOLD_LIMIT("CRMB025", "私海客户数已达上限，请先释放部分客户"),

    // ---- 联系人 / 跟进 ----
    CONTACT_CUSTOMER_REQUIRED("CRMB030", "联系人必须归属有效客户"),

    // ---- 商机 ----
    BUSINESS_FINISHED("CRMB040", "商机已结束（赢单/输单），不可再变更"),
    BUSINESS_STAGE_BACKWARD("CRMB041", "商机阶段只允许逐级推进，不可跳级或回退"),
    BUSINESS_NOT_WON("CRMB042", "只有赢单商机才能转为合同"),

    // ---- 合同 / 审批 ----
    CONTRACT_LOCKED("CRMB050", "合同当前状态不允许修改"),
    CONTRACT_MONEY_MISMATCH("CRMB051", "合同金额必须等于产品行折后合计"),
    CONTRACT_NOT_DRAFT("CRMB052", "只有草稿/已驳回合同可以提交审批"),
    CONTRACT_RECEIVABLE_EXCEED("CRMB053", "回款金额超出合同未收款"),
    APPROVAL_NOT_PENDING("CRMB060", "审批单不在审批中状态"),
    APPROVAL_NOT_CURRENT_NODE("CRMB061", "当前审批节点不允许该操作"),
    APPROVAL_ALREADY_DONE("CRMB062", "业务单据已存在在途审批"),

    // ---- 回款 / 发票 ----
    PLAN_ALREADY_SETTLED("CRMB070", "回款计划已结清，不可再登记"),
    PLAN_LOCKED("CRMB071", "已回款的计划期次不可修改"),
    INVOICE_LOCKED("CRMB072", "发票当前状态不允许该操作"),

    // ---- 售后 ----
    TICKET_CLOSED("CRMB080", "工单已关闭，不可再操作"),
    TICKET_NOT_OPEN("CRMB081", "工单不处于待受理状态"),
    TICKET_NOT_RESOLVED("CRMB082", "工单尚未解决，不能评价"),

    // ---- AI ----
    AI_NOT_AVAILABLE("CRMB090", "AI 服务当前不可用"),

    // ---- 条件查询(动态高级筛选) ----
    QUERY_FIELD_NOT_ALLOWED("CRMB100", "字段不允许查询"),
    OPERATOR_TYPE_MISMATCH("CRMB101", "算子与字段类型不匹配");

    private final String code;
    private final String message;
}
