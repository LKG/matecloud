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
package vip.mate.auth.domain.adapter.port;

import java.util.Map;

/**
 * Outbound port for delivering authentication-related notifications (verify
 * codes, login alerts, etc). The infrastructure layer provides one of:
 * <ul>
 *   <li>{@code DubboNoticeDispatcher} — when {@code mate.rpc.mode=dubbo}</li>
 *   <li>{@code LocalNoticeDispatcher} (in mate-monolith) — when {@code mate.rpc.mode=local}</li>
 * </ul>
 *
 * <p>If neither is on the classpath the dispatch falls through to a log-only
 * fallback inside {@code RedisSmsCodePort} so local dev keeps working.
 *
 * @author mateaix
 */
public interface NoticeDispatcher {

    /**
     * Dispatch an SMS for an authentication flow with a pre-rendered body.
     * Cloud SMS gateways (Aliyun / Tencent) prefer the template variant.
     *
     * @return true if the gateway accepted the request, false otherwise
     */
    boolean dispatchSms(String mobile, String content, String businessType);

    /**
     * Dispatch an SMS using structured template parameters — the variant the
     * cloud SMS APIs natively expect. Default implementation rejects so
     * implementations must opt in.
     */
    default boolean dispatchSmsTemplate(String mobile, String businessType,
                                        Map<String, String> templateParams) {
        return false;
    }
}
