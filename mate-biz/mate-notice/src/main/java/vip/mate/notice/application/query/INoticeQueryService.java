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
package vip.mate.notice.application.query;

import vip.mate.base.result.PageResult;

import java.time.LocalDateTime;

public interface INoticeQueryService {

    PageResult<NoticeView> page(int pageNum, int pageSize,
                                String channel, Integer status,
                                String businessType, String keyword);

    /**
     * Flat read model for the notice-center list. {@code status} is the code
     * (0=PENDING/1=SUCCESS/2=FAILED); {@code statusLabel} is its description.
     */
    record NoticeView(String id, String channel, String target, Integer status,
                      String statusLabel, String businessType, String content,
                      String resultMessage, Integer retryCount, LocalDateTime sentAt, LocalDateTime createdAt) {}
}
