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
package vip.mate.notice.application.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.mate.notice.domain.adapter.port.INoticeAdapter;
import vip.mate.notice.domain.adapter.port.INoticeAdapterFactory;
import vip.mate.notice.domain.adapter.repository.INoticeRepository;
import vip.mate.notice.domain.model.aggregate.NoticeAggregate;
import vip.mate.notice.types.enums.BusinessType;
import vip.mate.notice.types.enums.NoticeChannel;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeCommandService {

    private final INoticeRepository noticeRepository;
    private final INoticeAdapterFactory adapterFactory;

    @Transactional(rollbackFor = Exception.class)
    public String send(NoticeChannel channel, String target, BusinessType businessType, String content) {
        return send(channel, target, businessType, content, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public String send(NoticeChannel channel, String target, BusinessType businessType,
                       String content, Map<String, String> templateParams) {
        NoticeAggregate notice = NoticeAggregate.create(channel, target, businessType, content);
        notice.setTemplateParams(templateParams);
        noticeRepository.save(notice);

        try {
            INoticeAdapter adapter = adapterFactory.getAdapter(channel);
            boolean success = adapter.send(notice);
            if (success) {
                notice.markAsSuccess("OK");
            } else {
                notice.markAsFailed("Adapter returned false");
            }
        } catch (Exception e) {
            log.error("[mate-notice] send failed: id={}", notice.getId(), e);
            notice.markAsFailed(e.getMessage());
        }
        noticeRepository.update(notice);
        return notice.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void retry(String noticeId) {
        NoticeAggregate notice = noticeRepository.findById(noticeId);
        if (notice == null || !notice.canRetry()) return;

        notice.incrementRetry();
        try {
            INoticeAdapter adapter = adapterFactory.getAdapter(notice.getChannel());
            boolean success = adapter.send(notice);
            if (success) {
                notice.markAsSuccess("OK (retry)");
            } else {
                notice.markAsFailed("Retry failed");
            }
        } catch (Exception e) {
            notice.markAsFailed("Retry error: " + e.getMessage());
        }
        noticeRepository.update(notice);
    }
}
