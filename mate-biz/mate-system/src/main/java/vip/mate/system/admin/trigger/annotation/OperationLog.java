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
package vip.mate.system.admin.trigger.annotation;

import java.lang.annotation.*;

/**
 * Marks a controller method for automatic operation-log recording.
 *
 * <p>The AOP aspect captures request metadata, serialises safe arguments,
 * and publishes an async Spring event — the actual DB insert never blocks
 * the request thread.
 *
 * @author mateaix
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /** Business module, e.g. "管理员", "角色", "菜单". */
    String module();

    /** Operation type, e.g. "新增", "修改", "删除", "导入". */
    String type();

    /** Optional human-readable description shown in log detail. */
    String description() default "";
}
