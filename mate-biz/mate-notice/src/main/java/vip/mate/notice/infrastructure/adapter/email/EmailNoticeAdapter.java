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
package vip.mate.notice.infrastructure.adapter.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import vip.mate.notice.domain.adapter.port.INoticeAdapter;
import vip.mate.notice.domain.model.aggregate.NoticeAggregate;

/**
 * Email notification adapter using Spring Mail.
 *
 * @author mateaix
 */
@Slf4j
@Component("emailNoticeAdapter")
public class EmailNoticeAdapter implements INoticeAdapter {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Override
    public boolean send(NoticeAggregate notice) {
        if (mailSender == null) {
            log.warn("[mate-notice][EMAIL] JavaMailSender not configured; skipping");
            return false;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(notice.getTarget());
            message.setSubject("[" + notice.getBusinessType().getCode() + "] MateCloud Notification");
            message.setText(notice.getContent());
            mailSender.send(message);
            log.info("[mate-notice][EMAIL] sent to {}", notice.getTarget());
            return true;
        } catch (Exception e) {
            log.error("[mate-notice][EMAIL] failed: {}", e.getMessage(), e);
            return false;
        }
    }
}
