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
package vip.mate.crm.workbench.types.enums;

/**
 * 待办任务状态: 待办 → 已完成 / 已取消。
 *
 * @author mateaix
 */
public enum TaskStatus {

    OPEN,
    DONE,
    CANCELED;

    public boolean isOpen() {
        return this == OPEN;
    }

    public static TaskStatus of(String name) {
        for (TaskStatus s : values()) {
            if (s.name().equalsIgnoreCase(name)) {
                return s;
            }
        }
        throw new IllegalArgumentException("未知任务状态: " + name);
    }
}
