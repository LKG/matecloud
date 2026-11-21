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
package vip.mate.crm.trade.types.enums;

/**
 * 合同状态机: 草稿 → 审批中 → 通过/驳回 → 归档。
 *
 * <p>可编辑(改产品行/金额): DRAFT / REJECTED; 提交审批: DRAFT / REJECTED → PENDING;
 * 审批回写: PENDING → APPROVED / REJECTED; 生效归档: APPROVED → ARCHIVED。
 *
 * @author mateaix
 */
public enum ContractStatus {

    DRAFT,
    PENDING,
    APPROVED,
    REJECTED,
    ARCHIVED;

    /** 是否可编辑基本信息与产品行。 */
    public boolean isEditable() {
        return this == DRAFT || this == REJECTED;
    }

    /** 是否可提交审批。 */
    public boolean canSubmit() {
        return this == DRAFT || this == REJECTED;
    }

    public static ContractStatus of(String name) {
        for (ContractStatus s : values()) {
            if (s.name().equalsIgnoreCase(name)) {
                return s;
            }
        }
        throw new IllegalArgumentException("未知合同状态: " + name);
    }
}
