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
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Flat row used by the {@code GET /users/export} endpoint.
 *
 * <p>EasyExcel maps each {@code @ExcelProperty}-annotated field to a column
 * in declaration order; keep the column labels short (they become the header
 * row) and keep this class deliberately flat — nested objects require the
 * {@code @ContentRowHeight} trickery which isn't worth it for a demo export.
 *
 * @author mateaix
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserExportRow {

    @ExcelProperty("用户名")
    @ColumnWidth(18)
    private String username;

    @ExcelProperty("姓名")
    @ColumnWidth(16)
    private String realName;

    @ExcelProperty("手机号")
    @ColumnWidth(16)
    private String mobile;

    @ExcelProperty("邮箱")
    @ColumnWidth(28)
    private String email;

    @ExcelProperty("状态")
    @ColumnWidth(10)
    private String status;

    @ExcelProperty("创建时间")
    @ColumnWidth(22)
    private String createdAt;
}
