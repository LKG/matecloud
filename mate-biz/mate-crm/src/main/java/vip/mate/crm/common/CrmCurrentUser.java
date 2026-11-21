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
package vip.mate.crm.common;

/**
 * 当前登录用户快照(由 {@link CurrentUserResolver} 从 Sa-Token 会话解析)。
 *
 * @param userId   登录用户ID(loginId)
 * @param userName 登录账号
 * @param realName 姓名(冗余进 owner_name/creator_name 展示列)
 * @param deptId   所属部门ID(数据权限过滤列)
 */
public record CrmCurrentUser(String userId, String userName, String realName, String deptId) {

    /** 展示名优先取 realName, 回退 username。 */
    public String displayName() {
        return realName != null && !realName.isBlank() ? realName : userName;
    }
}
