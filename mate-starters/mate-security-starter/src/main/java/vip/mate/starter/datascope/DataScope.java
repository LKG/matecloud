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
package vip.mate.starter.datascope;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Data permission scope levels.
 *
 * @author mateaix
 */
@Getter
@AllArgsConstructor
public enum DataScope {

    ALL(1, "All data"),
    DEPT(2, "Current department only"),
    DEPT_AND_CHILD(3, "Current department and children"),
    SELF(4, "Self only"),
    CUSTOM(5, "Custom department list");

    private final int code;
    private final String description;

    public static DataScope of(int code) {
        for (DataScope scope : values()) {
            if (scope.code == code) return scope;
        }
        return ALL;
    }
}
