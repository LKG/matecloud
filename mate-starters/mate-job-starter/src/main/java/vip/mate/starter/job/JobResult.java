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
package vip.mate.starter.job;

import com.xxl.job.core.context.XxlJobHelper;

/**
 * Utility for reporting job execution results to XXL-Job.
 * Wraps XxlJobHelper static methods for cleaner business code.
 *
 * @author mateaix
 */
public final class JobResult {
    private JobResult() {}

    /** Report success with message. */
    public static void success(String msg) {
        XxlJobHelper.handleSuccess(msg);
    }

    /** Report success with formatted message. */
    public static void success(String format, Object... args) {
        XxlJobHelper.handleSuccess(String.format(format, args));
    }

    /** Report failure with message. */
    public static void fail(String msg) {
        XxlJobHelper.handleFail(msg);
    }

    /** Report failure with formatted message. */
    public static void fail(String format, Object... args) {
        XxlJobHelper.handleFail(String.format(format, args));
    }

    /** Get job parameter string. */
    public static String getJobParam() {
        return XxlJobHelper.getJobParam();
    }

    /** Get shard index (for sharding broadcast). */
    public static int getShardIndex() {
        return XxlJobHelper.getShardIndex();
    }

    /** Get total shard count (for sharding broadcast). */
    public static int getShardTotal() {
        return XxlJobHelper.getShardTotal();
    }

    /** Log to XXL-Job admin console. */
    public static void log(String msg) {
        XxlJobHelper.log(msg);
    }

    /** Log to XXL-Job admin console with format. */
    public static void log(String format, Object... args) {
        XxlJobHelper.log(format, args);
    }
}
