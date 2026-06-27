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
package vip.mate.notice.trigger.rpc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import vip.mate.api.notice.service.IRpcNoticeService;
import vip.mate.api.rpc.RpcConstants;
import vip.mate.base.response.ResponseCode;
import vip.mate.base.result.Result;
import vip.mate.notice.application.command.NoticeCommandService;
import vip.mate.notice.types.enums.BusinessType;
import vip.mate.notice.types.enums.NoticeChannel;

import java.util.Map;

@Slf4j
@DubboService(version = RpcConstants.VERSION, group = RpcConstants.GROUP_NOTICE)
@RequiredArgsConstructor
public class RpcNoticeServiceImpl implements IRpcNoticeService {

    private final NoticeCommandService noticeCommandService;

    @Override
    public Result<String> sendSms(String mobile, String content, String businessType) {
        BusinessType type = parseBusinessType(businessType);
        if (type == null) {
            return Result.fail(ResponseCode.PARAM_VALID_ERROR.getCode(),
                    "Unknown businessType: " + businessType);
        }
        try {
            String id = noticeCommandService.send(NoticeChannel.SMS, mobile, type, content);
            return Result.ok(id);
        } catch (Exception e) {
            log.error("[mate-notice] sendSms failed: mobile={}", mobile, e);
            return Result.fail(ResponseCode.INTERNAL_ERROR.getCode(), e.getMessage());
        }
    }

    @Override
    public Result<String> sendSmsTemplate(String mobile, String businessType,
                                          Map<String, String> templateParams) {
        BusinessType type = parseBusinessType(businessType);
        if (type == null) {
            return Result.fail(ResponseCode.PARAM_VALID_ERROR.getCode(),
                    "Unknown businessType: " + businessType);
        }
        // Build a human-readable content as a fallback for log providers.
        String content = renderFallbackContent(templateParams);
        try {
            String id = noticeCommandService.send(NoticeChannel.SMS, mobile, type,
                    content, templateParams);
            return Result.ok(id);
        } catch (Exception e) {
            log.error("[mate-notice] sendSmsTemplate failed: mobile={}", mobile, e);
            return Result.fail(ResponseCode.INTERNAL_ERROR.getCode(), e.getMessage());
        }
    }

    private static String renderFallbackContent(Map<String, String> params) {
        if (params == null || params.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("[SMS] ");
        params.forEach((k, v) -> sb.append(k).append('=').append(v).append(' '));
        return sb.toString().trim();
    }

    @Override
    public Result<String> sendEmail(String email, String subject, String content, String businessType) {
        BusinessType type = parseBusinessType(businessType);
        if (type == null) {
            return Result.fail(ResponseCode.PARAM_VALID_ERROR.getCode(),
                    "Unknown businessType: " + businessType);
        }
        // Subject is part of the email body for the simple SimpleMailMessage adapter;
        // when JavaMailSender + MIME is wired we can split this out cleanly.
        String body = subject == null || subject.isBlank()
                ? content
                : "[" + subject + "]\n" + content;
        try {
            String id = noticeCommandService.send(NoticeChannel.EMAIL, email, type, body);
            return Result.ok(id);
        } catch (Exception e) {
            log.error("[mate-notice] sendEmail failed: email={}", email, e);
            return Result.fail(ResponseCode.INTERNAL_ERROR.getCode(), e.getMessage());
        }
    }

    private BusinessType parseBusinessType(String code) {
        if (code == null || code.isBlank()) return BusinessType.VERIFY_CODE;
        try {
            return BusinessType.valueOf(code);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
