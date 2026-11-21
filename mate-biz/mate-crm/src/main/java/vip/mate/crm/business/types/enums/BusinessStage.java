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
package vip.mate.crm.business.types.enums;

/**
 * 商机阶段(销售漏斗): 只允许逐级向前推进, WIN/LOSE 为终态。
 *
 * <p>序: INITIAL → DEMAND → PLAN → NEGOTIATION → WIN; LOSE 可从任一在途阶段进入。
 *
 * @author mateaix
 */
public enum BusinessStage {

    INITIAL(0),
    DEMAND(1),
    PLAN(2),
    NEGOTIATION(3),
    WIN(4),
    LOSE(-1);

    private final int rank;

    BusinessStage(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }

    public boolean isTerminal() {
        return this == WIN || this == LOSE;
    }

    /** 是否为「活跃推进链」上的一步(不含 WIN/LOSE)。 */
    public boolean isActive() {
        return rank >= 0 && this != WIN;
    }

    /** 下一个活跃阶段; NEGOTIATION 之后无(需走 win)。 */
    public BusinessStage nextActive() {
        return switch (this) {
            case INITIAL -> DEMAND;
            case DEMAND -> PLAN;
            case PLAN -> NEGOTIATION;
            default -> null;
        };
    }

    public static BusinessStage of(String name) {
        for (BusinessStage s : values()) {
            if (s.name().equalsIgnoreCase(name)) {
                return s;
            }
        }
        throw new IllegalArgumentException("未知商机阶段: " + name);
    }
}
