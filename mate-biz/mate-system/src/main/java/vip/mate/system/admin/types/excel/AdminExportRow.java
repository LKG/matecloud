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
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Flat row used by {@code GET /admin/admins/export}.
 *
 * <p>Mirrors {@link vip.mate.system.types.excel.UserExportRow user-side} in
 * shape so admins can roughly compare the two sheets side-by-side. Status is
 * exported as a Chinese label rather than the raw enum name to match what
 * the UI shows in the list.
 *
 * @author mateaix
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminExportRow {

    @ExcelProperty("用户名")
    @ColumnWidth(18)
    private String username;

    @ExcelProperty("昵称")
    @ColumnWidth(16)
    private String nickName;

    @ExcelProperty("状态")
    @ColumnWidth(10)
    private String status;

    @ExcelProperty("创建时间")
    @ColumnWidth(22)
    private String createdAt;
}
