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
package vip.mate.starter.lock;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Annotation-driven distributed lock.
 *
 * @author mateaix
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {

    String name();

    /** 锁类型: 可重入(默认) / 公平 / 读 / 写。 */
    LockType lockType() default LockType.REENTRANT;

    /** 在 {@link #waitTime()} 内未拿到锁时的策略: 抛异常(默认) / 返回 null。 */
    LockTimeoutStrategy timeoutStrategy() default LockTimeoutStrategy.FAIL;

    long waitTime() default 3;

    /**
     * Lease time. Default {@code -1} enables Redisson's watchdog, which keeps
     * renewing the lease while the method runs so the lock cannot expire
     * mid-execution (and is auto-released shortly after the JVM dies). Set a
     * positive value only if you deliberately want a hard, non-renewed lease.
     */
    long leaseTime() default -1;

    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
