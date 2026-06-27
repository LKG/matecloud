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
package vip.mate.starter.datascope;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enable data permission filtering on a query method.
 *
 * @author mateaix
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataPermission {

    String deptAlias() default "dept_id";

    String userAlias() default "creator_id";

    /**
     * Tables this scope applies to. When non-empty, the interceptor only
     * rewrites a query whose SQL references one of these tables (matched on a
     * word boundary, so {@code mate_admin} does NOT match {@code mate_admin_role}).
     * Leave empty to apply to every query executed within the annotated method
     * (legacy behaviour — only safe when the method touches a single table).
     */
    String[] includeTables() default {};
}
