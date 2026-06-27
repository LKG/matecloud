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
package vip.mate.system.application.query;

import vip.mate.api.system.response.UserInfoResponse;
import vip.mate.base.result.PageResult;
import vip.mate.system.types.excel.UserExportRow;

import java.util.List;

public interface IUserQueryService {

    UserInfoResponse findById(String userId);

    UserInfoResponse findByUsername(String username);

    UserInfoResponse findByMobile(String mobile);

    /**
     * Find user by username including the hashed password (used only for auth RPC).
     */
    UserInfoResponse findByUsernameForAuth(String username);

    /**
     * Find user by mobile including the hashed password (used only for auth RPC).
     */
    UserInfoResponse findByMobileForAuth(String mobile);

    PageResult<UserInfoResponse> pageQuery(String keyword, int pageNum, int pageSize);

    /**
     * Flat rows for Excel export. {@code keyword} is a substring over
     * username/realName/mobile; {@code null} returns everything (capped
     * internally at {@value #EXPORT_MAX} rows to keep memory bounded).
     */
    List<UserExportRow> exportRows(String keyword);

    /** Hard cap on a single export to keep memory bounded. */
    int EXPORT_MAX = 50_000;
}
