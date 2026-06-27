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
package vip.mate.starter.distribute.util;

import cn.hutool.core.util.IdUtil;
import vip.mate.starter.distribute.worker.WorkerIdHolder;

/**
 * Snowflake ID generation utility.
 * <p>
 * Produces globally unique, time-ordered String IDs using Hutool's Snowflake.
 * The worker ID is auto-assigned from Redis on JVM startup by {@link WorkerIdHolder}.
 *
 * <pre>{@code
 *   String id = SnowflakeUtil.newSnowflakeId();
 * }</pre>
 *
 * @author mateaix
 */
public final class SnowflakeUtil {

    private SnowflakeUtil() {
    }

    /**
     * Generate a new Snowflake ID as a String.
     */
    public static String newSnowflakeId() {
        return IdUtil.getSnowflake(WorkerIdHolder.WORKER_ID).nextIdStr();
    }

    /**
     * Generate a new Snowflake ID as a long.
     */
    public static long newSnowflakeIdLong() {
        return IdUtil.getSnowflake(WorkerIdHolder.WORKER_ID).nextId();
    }
}
