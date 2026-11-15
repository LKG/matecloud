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

import java.util.Set;

/**
 * 单特性灰度规则快照（不可变值对象，从 Redis 读取后短 TTL 缓存）。
 *
 * <p>缺失字段一律取"最保守"默认值：未启用、0% 放量、空白名单 ——
 * 即任何解析不完整的规则都等价于"全部走稳定版"。</p>
 *
 * @param feature      特性 key
 * @param enabled      规则总开关
 * @param grayVersion  灰度版本号（命中时 {@link GrayContext} 里的值）
 * @param percentage   放量百分比 0-100，按 userId 稳定分桶
 * @param whitelist    用户白名单（无视百分比直接命中）
 * @author mateaix
 */
public record GrayRule(String feature, boolean enabled, String grayVersion,
                       int percentage, Set<String> whitelist) {

    /** 无规则 —— 等价于关闭灰度。 */
    public static GrayRule none(String feature) {
        return new GrayRule(feature, false, "", 0, Set.of());
    }

    /** 某用户是否命中：白名单直通，否则按 floorMod(hash) 稳定分桶。 */
    public boolean hit(String userId) {
        if (!enabled || grayVersion == null || grayVersion.isBlank()) {
            return false;
        }
        if (userId == null || userId.isBlank()) {
            return false;
        }
        if (whitelist.contains(userId)) {
            return true;
        }
        return percentage > 0 && Math.floorMod(userId.hashCode(), 100) < percentage;
    }
}
