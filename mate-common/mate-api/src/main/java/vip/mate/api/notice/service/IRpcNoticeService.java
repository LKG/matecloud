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
package vip.mate.api.notice.service;

import vip.mate.base.result.Result;

import java.util.Map;

/**
 * Dubbo RPC interface for notification dispatch.
 *
 * <p>Provider: mate-notice (group={@code notice}). Consumer: any service that
 * needs to send transactional notifications (mate-auth for SMS verification
 * codes, mate-system for password reset emails, etc).
 *
 * <p>The interface is intentionally narrow — it exposes one method per channel
 * rather than a generic {@code send(channel, ...)} so that callers don't need
 * to import notice-domain enums.
 *
 * @author mateaix
 */
public interface IRpcNoticeService {

    /**
     * Send an SMS to the given mobile number.
     *
     * @param mobile       11-digit mobile number (China) or E.164 format
     * @param content      SMS body text
     * @param businessType business code, one of: VERIFY_CODE / ORDER_NOTIFY /
     *                     SYSTEM_ALERT / MARKETING. Used by the SMS gateway
     *                     to pick the correct template.
     * @return notice ID on success
     */
    Result<String> sendSms(String mobile, String content, String businessType);

    /**
     * Send an SMS using structured template parameters. Required by Aliyun /
     * Tencent gateways which expect a JSON object such as
     * {@code {"code":"123456"}} rather than a rendered string.
     *
     * @param mobile         11-digit mobile number (China) or E.164 format
     * @param businessType   business code (used to look up the template ID)
     * @param templateParams ordered name → value map for template substitution
     * @return notice ID on success
     */
    Result<String> sendSmsTemplate(String mobile, String businessType,
                                   Map<String, String> templateParams);

    /**
     * Send an email to the given address.
     *
     * @param email        recipient email address
     * @param subject      email subject line
     * @param content      email body (plain text)
     * @param businessType business code, see {@link #sendSms}
     */
    Result<String> sendEmail(String email, String subject, String content, String businessType);
}
