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
package vip.mate.starter.rediskey;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MateRedisKeyTest {

    @AfterEach
    void resetPrefix() {
        MateRedisKey.setPrefix("");
    }

    @Test
    void buildsTemplatedKey() {
        assertEquals("user:login:1001", MateRedisKey.of("user:login:%s", 1001));
        assertEquals("order:1:item:2", MateRedisKey.of("order:%s:item:%s", 1, 2));
    }

    @Test
    void appliesGlobalPrefix() {
        MateRedisKey.setPrefix("mate-system:");
        assertEquals("mate-system:user:login:1001", MateRedisKey.of("user:login:%s", 1001));
    }

    @Test
    void noArgTemplatePassesThrough() {
        assertEquals("captcha:all", MateRedisKey.of("captcha:all"));
    }

    @Test
    void argCountMismatchThrows() {
        // 少参
        assertThrows(IllegalArgumentException.class, () -> MateRedisKey.of("user:%s:role:%s", 1));
        // 多参 (String.format 会静默吞掉, 这里主动拦)
        assertThrows(IllegalArgumentException.class, () -> MateRedisKey.of("user:%s", 1, 2));
    }

    @Test
    void buildsFromRedisKeyDef() {
        RedisKeyDef def = () -> "session:%s";
        assertEquals("session:abc", MateRedisKey.of(def, "abc"));
    }
}
