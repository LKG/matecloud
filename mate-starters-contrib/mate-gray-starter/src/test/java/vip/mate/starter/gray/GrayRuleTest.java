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

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link GrayRule} 命中语义单测 —— 灰度决策的核心算法（白名单 + 稳定分桶 + 保守兜底）。
 *
 * @author mateaix
 */
class GrayRuleTest {

    private static GrayRule rule(boolean enabled, String version, int percent, Set<String> whitelist) {
        return new GrayRule("f", enabled, version, percent, whitelist);
    }

    @Test
    void noneNeverHits() {
        assertFalse(GrayRule.none("f").hit("1"));
    }

    @Test
    void disabledRuleNeverHits() {
        assertFalse(rule(false, "2.0.0", 100, Set.of("1")).hit("1"));
    }

    @Test
    void blankVersionNeverHits() {
        assertFalse(rule(true, "", 100, Set.of("1")).hit("1"));
    }

    @Test
    void anonymousAlwaysStable() {
        assertFalse(rule(true, "2.0.0", 100, Set.of("1")).hit(null));
        assertFalse(rule(true, "2.0.0", 100, Set.of("1")).hit(" "));
    }

    @Test
    void whitelistHitsRegardlessOfPercent() {
        assertTrue(rule(true, "2.0.0", 0, Set.of("42")).hit("42"));
    }

    @Test
    void percentZeroKeepsNonWhitelistedOut() {
        assertFalse(rule(true, "2.0.0", 0, Set.of()).hit("42"));
    }

    @Test
    void percentHundredTakesEveryone() {
        assertTrue(rule(true, "2.0.0", 100, Set.of()).hit("anything"));
    }

    @Test
    void bucketingIsStableAndMatchesFloorMod() {
        String userId = "10086";
        int bucket = Math.floorMod(userId.hashCode(), 100);
        // 恰好卡在桶值两侧：p=bucket 不含，p=bucket+1 必含。
        assertFalse(rule(true, "2.0.0", bucket, Set.of()).hit(userId));
        assertTrue(rule(true, "2.0.0", bucket + 1, Set.of()).hit(userId));
        // 同一用户任意多次决策结果一致（稳定分桶）。
        GrayRule r = rule(true, "2.0.0", 30, Set.of());
        boolean first = r.hit(userId);
        for (int i = 0; i < 10; i++) {
            assertTrue(r.hit(userId) == first);
        }
    }
}
