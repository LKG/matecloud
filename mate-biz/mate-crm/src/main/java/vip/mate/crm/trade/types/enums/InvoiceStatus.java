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
 * 发票状态(存 TINYINT): 已申请/已开票/已驳回/已作废。
 *
 * @author mateaix
 */
public enum InvoiceStatus {

    APPLIED(0),
    ISSUED(1),
    REJECTED(2),
    VOID(3);

    private final int code;

    InvoiceStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    /** 仅已申请的发票可执行开票/驳回。 */
    public boolean canProcess() {
        return this == APPLIED;
    }

    public static InvoiceStatus of(int code) {
        for (InvoiceStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return APPLIED;
    }
}
