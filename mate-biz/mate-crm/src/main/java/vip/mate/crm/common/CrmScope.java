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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 数据权限快照(本人/部门/部门及下级/自定义/全部), 由各仓储实现在 wrapper 上应用。
 *
 * <p>code 语义与 mate_role.data_scope 一致: 1全部 2本部门 3本部门及下级 4本人 5自定义。
 * 读取自 Sa-Token 会话(mate-system DataScopeSessionInterceptor 写入); 会话缺失
 * (任务线程/未解析)按「全部」处理, 与平台既有回退行为一致。
 *
 * @param code          范围码
 * @param userId        当前用户ID(selfOnly 用)
 * @param deptId        当前部门ID
 * @param customDeptIds 自定义部门集合
 */
public record CrmScope(int code, String userId, String deptId, List<String> customDeptIds) {

    public static final int ALL = 1;
    public static final int DEPT = 2;
    public static final int DEPT_AND_CHILD = 3;
    public static final int SELF = 4;
    public static final int CUSTOM = 5;

    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9]+");

    public static CrmScope all() {
        return new CrmScope(ALL, null, null, List.of());
    }

    /**
     * 在查询 wrapper 上追加「负责人列 + 部门列」范围过滤。
     * id 只允许字母数字(雪花/UUID), 非法值按不过滤处理(fail-open, 与平台一致)。
     */
    public <T> void apply(LambdaQueryWrapper<T> wrapper, SFunction<T, ?> ownerCol, SFunction<T, ?> deptCol) {
        switch (code) {
            case SELF -> {
                if (isSafe(userId)) {
                    wrapper.eq(ownerCol, userId);
                }
            }
            case DEPT -> {
                if (isSafe(deptId)) {
                    wrapper.eq(deptCol, deptId);
                }
            }
            case DEPT_AND_CHILD -> {
                if (isSafe(deptId)) {
                    wrapper.inSql(deptCol, "SELECT id FROM mate_dept WHERE id = '" + deptId
                            + "' OR ancestors LIKE '" + deptId + ",%'");
                }
            }
            case CUSTOM -> {
                List<String> ids = customDeptIds == null ? List.of()
                        : customDeptIds.stream().filter(CrmScope::isSafe).toList();
                if (!ids.isEmpty()) {
                    wrapper.in(deptCol, ids);
                }
            }
            default -> { } // ALL: 不加条件
        }
    }

    private static boolean isSafe(String id) {
        return id != null && SAFE_ID.matcher(id).matches();
    }
}
