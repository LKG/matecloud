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

import java.lang.annotation.*;

/**
 * Marks a Spring bean method as an XXL-Job handler.
 * This is a convenience alias for @XxlJob with additional MateCloud metadata.
 *
 * Usage:
 * <pre>
 * @Component
 * public class OrderJobs {
 *     @MateJobHandler(value = "orderTimeoutCheck", description = "Check and close timeout orders")
 *     public void checkTimeoutOrders() {
 *         // business logic
 *     }
 * }
 * </pre>
 *
 * @author mateaix
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MateJobHandler {
    /** Job handler name (must be unique across the application). */
    String value();

    /** Human-readable description for admin console. */
    String description() default "";

    /** Init method name (called once when handler is created). */
    String init() default "";

    /** Destroy method name (called once when handler is destroyed). */
    String destroy() default "";
}
