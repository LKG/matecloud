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
package vip.mate.notice.infrastructure.adapter.sms;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vip.mate.notice.domain.adapter.port.INoticeAdapter;
import vip.mate.notice.domain.model.aggregate.NoticeAggregate;
import vip.mate.starter.sms.core.SmsTemplate;
import vip.mate.starter.sms.model.SmsMessage;
import vip.mate.starter.sms.model.SmsResult;

/**
 * SMS notification adapter — delegates to {@code mate-sms-starter}'s
 * {@link SmsTemplate}, which routes to the provider selected by
 * {@code mate.sms.provider} (log / aliyun / tencent / custom). Structured
 * {@code templateParams} carried on the aggregate are forwarded as-is.
 *
 * @author mateaix
 */
@Slf4j
@Component("smsNoticeAdapter")
@RequiredArgsConstructor
public class SmsNoticeAdapter implements INoticeAdapter {

    private final SmsTemplate smsTemplate;

    @Override
    public boolean send(NoticeAggregate notice) {
        String businessType = notice.getBusinessType() == null
                ? "VERIFY_CODE"
                : notice.getBusinessType().getCode();
        SmsResult result = smsTemplate.send(SmsMessage.of(notice.getTarget(), notice.getContent(), businessType)
                .setTemplateParams(notice.getTemplateParams()));
        if (!result.success()) {
            log.warn("[mate-notice][sms] provider={} failed: target={} businessType={} msg={}",
                    result.providerType(), notice.getTarget(), businessType, result.message());
        }
        return result.success();
    }
}
