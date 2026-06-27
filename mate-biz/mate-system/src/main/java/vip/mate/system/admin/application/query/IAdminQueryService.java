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
package vip.mate.system.admin.application.query;

import vip.mate.api.system.response.UserInfoResponse;
import vip.mate.base.result.PageResult;
import vip.mate.system.admin.types.excel.AdminExportRow;

import java.time.LocalDateTime;
import java.util.List;

public interface IAdminQueryService {
    AdminVO findById(String id);
    PageResult<AdminVO> pageQuery(String keyword, int pageNum, int pageSize);

    // ---- Authentication lookups (mate_admin is the back-office account subject) ----
    // Return the full account INCLUDING the password hash + status so mate-auth
    // can verify credentials. Roles/permissions are resolved separately by the
    // auth flow via IRpcPermissionService#getRoleKeysByUsername.
    UserInfoResponse findForAuthByUsername(String username);
    UserInfoResponse findForAuthByMobile(String mobile);
    UserInfoResponse findForAuthById(String id);

    /**
     * Flat rows for Excel export. Capped at {@link #EXPORT_MAX} to keep the
     * response payload bounded — same contract as IUserQueryService#exportRows.
     */
    List<AdminExportRow> exportRows();

    /** Hard cap on a single export. */
    int EXPORT_MAX = 50_000;

    // status is the AdminStatus enum NAME ("ACTIVE"/"DISABLED") — string enum
    // names are the project-wide convention for status fields exposed to the UI.
    record AdminVO(String id, String username, String mobile, String email,
                   String nickName, String realName, String avatar,
                   String status, String deptId, List<String> roleIds, LocalDateTime createdAt) {}
}
