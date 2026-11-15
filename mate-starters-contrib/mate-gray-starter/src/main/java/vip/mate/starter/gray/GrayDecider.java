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
package vip.mate.starter.gray;

/**
 * 灰度版本决策端口：给定特性 key 与用户，返回应命中的灰度版本。
 *
 * <p>返回 {@code null} 表示走稳定版 —— 这是所有实现必须遵守的兜底语义
 * （规则缺失 / Redis 不可用 / 未登录 → 稳定版），保证灰度组件故障时
 * 行为与未接入灰度完全一致。</p>
 *
 * @author mateaix
 */
public interface GrayDecider {

    /**
     * @param feature 特性 key（如 {@code notice-center}），对应 Redis 规则
     *                {@code mate:gray:rule:{feature}}
     * @param userId  已登录用户 id（字符串形式）；可为 null（匿名请求）
     * @return 灰度版本号，或 null 表示稳定版
     */
    String decide(String feature, String userId);
}
