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
package vip.mate.system.domain.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStatus {

    ACTIVE(0, "Active"),
    FROZEN(1, "Frozen"),
    DELETED(2, "Deleted");

    private final int code;
    private final String desc;

    public boolean isActive() { return this == ACTIVE; }
    public boolean isFrozen() { return this == FROZEN; }
    public boolean isDeleted() { return this == DELETED; }
    public boolean canModify() { return this == ACTIVE; }

    public static UserStatus fromCode(Integer code) {
        if (code == null) return null;
        for (UserStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Unknown UserStatus code: " + code);
    }
}
