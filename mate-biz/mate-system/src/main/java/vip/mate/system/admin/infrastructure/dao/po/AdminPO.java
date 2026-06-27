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
package vip.mate.system.admin.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("mate_admin")
public class AdminPO {
    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    /**
     * Owning tenant. Whitelisted in {@code mate.tenant.include-tables}, so the
     * row-level interceptor scopes every mate_admin read/write to this value
     * once multi-tenancy is enabled. Column has DB default '1' (system tenant)
     * for legacy single-tenant rows.
     */
    @TableField("tenant_id")
    private String tenantId;
    private String username;
    private String password;
    private String mobile;
    private String email;
    @TableField("nick_name")
    private String nickName;
    @TableField("real_name")
    private String realName;
    private String avatar;
    private Integer status;
    @TableField("dept_id")
    private String deptId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
    // Reserved column. Optimistic locking is NOT wired (see DataSourceAutoConfiguration).
    @TableField(fill = FieldFill.INSERT)
    private Integer lockVersion;
}
