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
 * 回款计划期次状态(存 TINYINT): 待回款/部分/已回款/逾期。
 *
 * @author mateaix
 */
public enum PlanStatus {

    PENDING(0),
    PARTIAL(1),
    SETTLED(2),
    OVERDUE(3);

    private final int code;

    PlanStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    /** 是否已结清(不可再登记回款)。 */
    public boolean isSettled() {
        return this == SETTLED;
    }

    public static PlanStatus of(int code) {
        for (PlanStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return PENDING;
    }
}
