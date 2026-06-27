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
package vip.mate.starter.satoken;

import cn.dev33.satoken.stp.StpUtil;

/**
 * Strongly-typed helper around Sa-Token's session.
 *
 * @author mateaix
 */
public class StpUserUtil {

    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_REAL_NAME = "realName";
    private static final String KEY_TENANT_ID = "tenantId";
    private static final String KEY_ROLE_IDS = "roleIds";

    public Long getLoginId() {
        return StpUtil.getLoginIdAsLong();
    }

    public String getLoginIdAsString() {
        return StpUtil.getLoginIdAsString();
    }

    public String getTokenValue() {
        return StpUtil.getTokenValue();
    }

    public boolean isLogin() {
        return StpUtil.isLogin();
    }

    public Long getUserId() {
        return (Long) StpUtil.getSession().get(KEY_USER_ID);
    }

    public String getUsername() {
        return (String) StpUtil.getSession().get(KEY_USERNAME);
    }

    public String getRealName() {
        return (String) StpUtil.getSession().get(KEY_REAL_NAME);
    }

    public Long getTenantId() {
        Object val = StpUtil.getSession().get(KEY_TENANT_ID);
        return val != null ? (Long) val : null;
    }

    public String getRoleIds() {
        return (String) StpUtil.getSession().get(KEY_ROLE_IDS);
    }

    public void setUserSession(Long userId, String username, String realName,
                               Long tenantId, String roleIds) {
        StpUtil.getSession().set(KEY_USER_ID, userId);
        StpUtil.getSession().set(KEY_USERNAME, username);
        StpUtil.getSession().set(KEY_REAL_NAME, realName);
        StpUtil.getSession().set(KEY_TENANT_ID, tenantId);
        StpUtil.getSession().set(KEY_ROLE_IDS, roleIds);
    }

    public void logout() {
        StpUtil.logout();
    }

    public boolean hasRole(String role) {
        return StpUtil.hasRole(role);
    }

    public boolean hasPermission(String permission) {
        return StpUtil.hasPermission(permission);
    }
}
