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
package vip.mate.system.admin.types.security;

/**
 * Permission code constants used by Sa-Token's {@code @SaCheckPermission}
 * annotations across mate-admin controllers.
 *
 * <p>Naming convention: {@code sys:{entity}:{action}}. Wildcards
 * ({@code sys:*}, {@code *}) match too — Sa-Token resolves them at check time.
 *
 * <p>Frontend's {@code v-permission} directive consumes the same strings to
 * decide whether to render an action button — keep both ends in lockstep.
 *
 * @author mateaix
 */
public final class Perms {

    private Perms() {}

    // ---- Admins ----
    public static final String ADMIN_LIST   = "sys:admin:list";
    public static final String ADMIN_ADD    = "sys:admin:add";
    public static final String ADMIN_EDIT   = "sys:admin:edit";
    public static final String ADMIN_DELETE = "sys:admin:delete";
    public static final String ADMIN_RESET  = "sys:admin:reset";

    // ---- Roles ----
    public static final String ROLE_LIST   = "sys:role:list";
    public static final String ROLE_ADD    = "sys:role:add";
    public static final String ROLE_EDIT   = "sys:role:edit";
    public static final String ROLE_DELETE = "sys:role:delete";

    // ---- Menus ----
    public static final String MENU_LIST   = "sys:menu:list";
    public static final String MENU_ADD    = "sys:menu:add";
    public static final String MENU_EDIT   = "sys:menu:edit";
    public static final String MENU_DELETE = "sys:menu:delete";

    // ---- Dict (types + data share the same codes) ----
    public static final String DICT_LIST   = "sys:dict:list";
    public static final String DICT_ADD    = "sys:dict:add";
    public static final String DICT_EDIT   = "sys:dict:edit";
    public static final String DICT_DELETE = "sys:dict:delete";

    // ---- Config ----
    public static final String CONFIG_LIST   = "sys:config:list";
    public static final String CONFIG_ADD    = "sys:config:add";
    public static final String CONFIG_EDIT   = "sys:config:edit";
    public static final String CONFIG_DELETE = "sys:config:delete";

    // ---- Logs ----
    public static final String LOG_LIST   = "sys:log:list";
    public static final String LOG_DELETE = "sys:log:delete";

    // ---- Departments ----
    public static final String DEPT_LIST   = "sys:dept:list";
    public static final String DEPT_ADD    = "sys:dept:add";
    public static final String DEPT_EDIT   = "sys:dept:edit";
    public static final String DEPT_DELETE = "sys:dept:delete";
}
