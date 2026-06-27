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
package vip.mate.notice.application.query.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vip.mate.base.result.PageResult;
import vip.mate.notice.application.query.INoticeQueryService;
import vip.mate.notice.domain.adapter.repository.INoticeRepository;
import vip.mate.notice.domain.model.aggregate.NoticeAggregate;
import vip.mate.notice.types.enums.NoticeStatus;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeQueryServiceImpl implements INoticeQueryService {

    private final INoticeRepository noticeRepository;

    @Override
    public PageResult<NoticeView> page(int pageNum, int pageSize,
                                       String channel, Integer status,
                                       String businessType, String keyword) {
        PageResult<NoticeAggregate> raw = noticeRepository.page(
                pageNum, pageSize, channel, status, businessType, keyword);
        List<NoticeView> list = raw.getList().stream().map(this::toView).toList();
        return PageResult.of(list, raw.getTotal());
    }

    private NoticeView toView(NoticeAggregate n) {
        NoticeStatus st = n.getStatus();
        return new NoticeView(
                n.getId(),
                n.getChannel() == null ? null : n.getChannel().getCode(),
                n.getTarget(),
                st == null ? null : st.getCode(),
                st == null ? null : st.getDescription(),
                n.getBusinessType() == null ? null : n.getBusinessType().getCode(),
                n.getContent(),
                n.getResultMessage(),
                n.getRetryCount(),
                n.getSentAt(),
                n.getCreatedAt());
    }
}
