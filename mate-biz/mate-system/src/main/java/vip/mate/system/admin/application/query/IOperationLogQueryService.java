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

import vip.mate.base.result.PageResult;

import java.time.LocalDateTime;

public interface IOperationLogQueryService {

    /**
     * Paginated query with filters. All filter arguments may be {@code null}.
     *
     * @param module        exact match on module name
     * @param username      substring match on username
     * @param status        0=success, 1=failure
     * @param operationType exact match on operation type (新增/修改/删除/…)
     */
    PageResult<OperationLogVO> page(int pageNum, int pageSize,
                                     String module, String username,
                                     Integer status, String operationType,
                                     LocalDateTime startTime, LocalDateTime endTime);

    OperationLogDetail findById(String id);

    /** Summary VO returned in the list view. */
    record OperationLogVO(String id, String userId, String username, String module,
                          String operationType, String description, String requestMethod,
                          String requestUrl, String clientIp, Integer status, String errorMsg,
                          Long duration, LocalDateTime createdAt) {}

    /** Full detail with request/response body for the detail dialog. */
    record OperationLogDetail(String id, String userId, String username, String module,
                              String operationType, String description,
                              String requestMethod, String requestUrl,
                              String requestParams, String responseResult,
                              String clientIp, String userAgent, Integer status,
                              String errorMsg, Long duration, LocalDateTime createdAt) {}
}
