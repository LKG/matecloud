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
@TableName("mate_menu")
public class MenuPO {
    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    @TableField("parent_id")
    private String parentId;
    /** Stable key supplied by module self-registration (menu-manifest). */
    private String code;
    /** Owning module code (from mate.module.code). */
    @TableField("module_code")
    private String moduleCode;
    private String name;
    @TableField("name_en")
    private String nameEn;
    /** Optional short name for narrow rail rendering (two-column layout). */
    @TableField("short_name")
    private String shortName;
    private String path;
    private String component;
    private String perms;
    private String type;
    private String icon;
    private Integer sort;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
