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
package vip.mate.system.admin.types.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Flat row consumed by {@code POST /admin/admins/import}.
 *
 * <p>Three columns — username (required), password (required, BCrypt'd
 * server-side), nickName (optional, falls back to username). Roles are NOT
 * importable from a sheet by design: that's a sensitive RBAC concern best
 * driven through the UI's "assign roles" dialog where you can see the menu
 * tree at the same time.
 *
 * @author mateaix
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminImportRow {

    @ExcelProperty("用户名")
    private String username;

    @ExcelProperty("密码")
    private String password;

    @ExcelProperty("昵称")
    private String nickName;
}
