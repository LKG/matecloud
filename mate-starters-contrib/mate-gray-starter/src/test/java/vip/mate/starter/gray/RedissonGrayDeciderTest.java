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
import org.redisson.api.RMap;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link RedissonGrayDecider} 规则读取与降级语义单测 —— 重点验证
 * 「规则缺失 / Redis 故障 → 稳定版(null)」的保守兜底。
 *
 * @author mateaix
 */
class RedissonGrayDeciderTest {

    @SuppressWarnings("unchecked")
    private RedissonGrayDecider deciderWith(Map<String, String> fields, Set<String> whitelist) {
        RedissonClient redisson = mock(RedissonClient.class);
        RMap<String, String> map = mock(RMap.class);
        RSet<String> set = mock(RSet.class);
        doReturn(map).when(redisson).getMap(anyString(), eq(StringCodec.INSTANCE));
        when(map.readAllMap()).thenReturn(fields);
        doReturn(set).when(redisson).getSet(anyString(), eq(StringCodec.INSTANCE));
        lenient().when(set.isEmpty()).thenReturn(whitelist.isEmpty());
        lenient().when(set.readAll()).thenReturn(whitelist);
        return new RedissonGrayDecider(redisson, new GrayProperties());
    }

    @Test
    void missingRuleMeansStable() {
        assertNull(deciderWith(Map.of(), Set.of()).decide("default", "1"));
    }

    @Test
    void disabledRuleMeansStable() {
        var d = deciderWith(Map.of("enabled", "false", "grayVersion", "2.0.0", "percentage", "100"), Set.of());
        assertNull(d.decide("default", "1"));
    }

    @Test
    void whitelistUserGetsGrayVersion() {
        var d = deciderWith(Map.of("enabled", "true", "grayVersion", "2.0.0", "percentage", "0"), Set.of("7"));
        assertEquals("2.0.0", d.decide("default", "7"));
        assertNull(d.decide("default", "8"));
    }

    @Test
    void garbagePercentageFallsBackToZero() {
        var d = deciderWith(Map.of("enabled", "true", "grayVersion", "2.0.0", "percentage", "abc"), Set.of());
        assertNull(d.decide("default", "1"));
    }

    @Test
    void redisOutageMeansStable() {
        RedissonClient redisson = mock(RedissonClient.class);
        when(redisson.getMap(anyString(), eq(StringCodec.INSTANCE)))
                .thenThrow(new IllegalStateException("redis down"));
        var d = new RedissonGrayDecider(redisson, new GrayProperties());
        assertNull(d.decide("default", "1"));
    }
}
