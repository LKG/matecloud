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
package vip.mate.starter.sharding.algorithm;

import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.Collection;
import java.util.Properties;

/**
 * Table-level sharding algorithm: _0 through _3 based on (hash % 8) % 4.
 *
 * @author mateaix
 */
@Slf4j
public class MateTableShardingAlgorithm implements StandardShardingAlgorithm<Comparable<?>> {

    private static final int TOTAL_SHARDS = 8;
    private static final int TABLES_PER_DB = 4;

    private Properties props = new Properties();

    @Override
    public String doSharding(Collection<String> availableTargetNames,
                             PreciseShardingValue<Comparable<?>> shardingValue) {
        long shardingValueLong = toShardingLong(shardingValue.getValue());
        long shardIndex = Math.abs(shardingValueLong) % TOTAL_SHARDS;
        int tableSuffix = (int) (shardIndex % TABLES_PER_DB);
        String suffix = "_" + tableSuffix;
        for (String name : availableTargetNames) {
            if (name.endsWith(suffix)) {
                return name;
            }
        }
        return shardingValue.getLogicTableName() + suffix;
    }

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames,
                                         RangeShardingValue<Comparable<?>> shardingValue) {
        return availableTargetNames;
    }

    @Override
    public void init(Properties props) {
        this.props = props;
    }

    public String getType() {
        return "MATE_TABLE_HASH";
    }

    private long toShardingLong(Comparable<?> value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String strValue) {
            try {
                return Long.parseLong(strValue);
            } catch (NumberFormatException e) {
                return Math.abs(strValue.hashCode());
            }
        }
        return Math.abs(value.hashCode());
    }
}
