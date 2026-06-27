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
package vip.mate.monolith.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import vip.mate.auth.domain.adapter.port.NoticeDispatcher;
import vip.mate.notice.application.command.NoticeCommandService;
import vip.mate.notice.types.enums.BusinessType;
import vip.mate.notice.types.enums.NoticeChannel;

import java.util.Map;

/**
 * In-process implementation of {@link NoticeDispatcher} for monolith mode.
 * Calls mate-notice's command service directly instead of going through Dubbo.
 *
 * @author mateaix
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mate.rpc.mode", havingValue = "local")
public class LocalNoticeDispatcher implements NoticeDispatcher {

    private final NoticeCommandService noticeCommandService;

    @Override
    public boolean dispatchSms(String mobile, String content, String businessType) {
        try {
            BusinessType type = parseBusinessType(businessType);
            String noticeId = noticeCommandService.send(NoticeChannel.SMS, mobile, type, content);
            return noticeId != null;
        } catch (Exception e) {
            log.warn("[monolith] LocalNoticeDispatcher.dispatchSms failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean dispatchSmsTemplate(String mobile, String businessType,
                                       Map<String, String> templateParams) {
        try {
            BusinessType type = parseBusinessType(businessType);
            String content = renderFallbackContent(templateParams);
            String noticeId = noticeCommandService.send(NoticeChannel.SMS, mobile, type,
                    content, templateParams);
            return noticeId != null;
        } catch (Exception e) {
            log.warn("[monolith] LocalNoticeDispatcher.dispatchSmsTemplate failed: {}", e.getMessage());
            return false;
        }
    }

    private static String renderFallbackContent(Map<String, String> params) {
        if (params == null || params.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("[SMS] ");
        params.forEach((k, v) -> sb.append(k).append('=').append(v).append(' '));
        return sb.toString().trim();
    }

    private BusinessType parseBusinessType(String code) {
        if (code == null || code.isBlank()) return BusinessType.VERIFY_CODE;
        try {
            return BusinessType.valueOf(code);
        } catch (IllegalArgumentException e) {
            return BusinessType.VERIFY_CODE;
        }
    }
}
