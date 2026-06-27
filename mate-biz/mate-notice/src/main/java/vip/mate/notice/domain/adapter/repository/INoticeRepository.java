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
package vip.mate.notice.domain.adapter.repository;

import vip.mate.base.result.PageResult;
import vip.mate.notice.domain.model.aggregate.NoticeAggregate;

import java.util.List;

public interface INoticeRepository {

    void save(NoticeAggregate notice);

    void update(NoticeAggregate notice);

    NoticeAggregate findById(String id);

    List<NoticeAggregate> findFailedForRetry(int limit);

    /** Paginated notice records, newest first. Null filters are ignored. */
    PageResult<NoticeAggregate> page(int pageNum, int pageSize,
                                     String channel, Integer status,
                                     String businessType, String keyword);
}
