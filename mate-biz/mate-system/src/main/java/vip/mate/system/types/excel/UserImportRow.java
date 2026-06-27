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
package vip.mate.system.types.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Flat row shape consumed by {@code POST /users/import}.
 *
 * <p>EasyExcel populates fields by matching the Chinese column headers below
 * against the row-1 cells of the uploaded {@code .xlsx}. Extra columns are
 * ignored silently — that lets admins paste their own sheets as long as
 * these five headers exist somewhere (order doesn't matter either).
 *
 * <p>Validation is intentionally NOT done here with Bean Validation: EasyExcel
 * runs the row through Jackson-style binding, not the Spring validator chain,
 * so any constraints go unchecked. We re-validate every row explicitly inside
 * {@code UserCommandService.importBatch} instead — that keeps the error
 * reporting symmetric with the Excel row index shown to the user.
 *
 * <p>The {@code password} column is optional. When blank the backend falls
 * back to the seeded default (looked up via {@code sys.user.initPassword}
 * config) so a minimal sheet only needs the other four columns filled.
 *
 * @author mateaix
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserImportRow {

    @ExcelProperty("用户名")
    private String username;

    @ExcelProperty("手机号")
    private String mobile;

    @ExcelProperty("邮箱")
    private String email;

    @ExcelProperty("姓名")
    private String realName;

    @ExcelProperty("初始密码")
    private String password;
}
