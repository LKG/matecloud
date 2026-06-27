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
package vip.mate.auth.infrastructure.adapter.notice;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import vip.mate.api.notice.service.IRpcNoticeService;
import vip.mate.api.rpc.RpcConstants;
import vip.mate.auth.domain.adapter.port.NoticeDispatcher;
import vip.mate.base.result.Result;

import java.util.Map;

/**
 * NoticeDispatcher implementation backed by a Dubbo reference to mate-notice.
 * Active in microservice mode ({@code mate.rpc.mode=dubbo}, the default).
 *
 * @author mateaix
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "mate.rpc.mode", havingValue = "dubbo", matchIfMissing = true)
public class DubboNoticeDispatcher implements NoticeDispatcher {

    @DubboReference(version = RpcConstants.VERSION,
            group = RpcConstants.GROUP_NOTICE,
            timeout = RpcConstants.DEFAULT_TIMEOUT,
            retries = RpcConstants.NO_RETRY,
            check = false)
    private IRpcNoticeService noticeService;

    @Override
    public boolean dispatchSms(String mobile, String content, String businessType) {
        if (noticeService == null) {
            return false;
        }
        try {
            Result<String> result = noticeService.sendSms(mobile, content, businessType);
            return result != null && Boolean.TRUE.equals(result.getSuccess());
        } catch (Exception e) {
            log.warn("[auth] Dubbo notice dispatch failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean dispatchSmsTemplate(String mobile, String businessType,
                                       Map<String, String> templateParams) {
        if (noticeService == null) {
            return false;
        }
        try {
            Result<String> result = noticeService.sendSmsTemplate(mobile, businessType, templateParams);
            return result != null && Boolean.TRUE.equals(result.getSuccess());
        } catch (Exception e) {
            log.warn("[auth] Dubbo notice template dispatch failed: {}", e.getMessage());
            return false;
        }
    }
}
