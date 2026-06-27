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
package vip.mate.auth.infrastructure.adapter.sms;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import vip.mate.auth.domain.adapter.port.NoticeDispatcher;
import vip.mate.auth.domain.adapter.port.SmsCodePort;
import vip.mate.base.exception.BizException;
import vip.mate.base.response.ResponseCode;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Redis-backed SMS code port. Generates a 6-digit code, stores it with a
 * 5-minute TTL, and enforces a 1-per-minute rate limit. Delivery is delegated
 * to the active {@link NoticeDispatcher} (Dubbo or local). When no dispatcher
 * is wired the code is only logged — Redis still holds it, so verifyLoginCode
 * works end-to-end for local dev / unit tests.
 *
 * @author mateaix
 */
@Slf4j
@Component
public class RedisSmsCodePort implements SmsCodePort {

    private static final String CODE_KEY_PREFIX = "mate:auth:sms:code:";
    private static final String RATE_KEY_PREFIX = "mate:auth:sms:rate:";
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration RATE_TTL = Duration.ofMinutes(1);
    private static final String SMS_TEMPLATE = "Your MateCloud verification code is %s. Valid for 5 minutes.";

    private final RedissonClient redissonClient;
    private final ObjectProvider<NoticeDispatcher> noticeDispatcher;

    public RedisSmsCodePort(RedissonClient redissonClient,
                            ObjectProvider<NoticeDispatcher> noticeDispatcher) {
        this.redissonClient = redissonClient;
        this.noticeDispatcher = noticeDispatcher;
    }

    @Override
    public void sendLoginCode(String mobile) {
        RBucket<String> rate = redissonClient.getBucket(RATE_KEY_PREFIX + mobile);
        if (!rate.setIfAbsent("1", RATE_TTL)) {
            throw new BizException(ResponseCode.TOO_MANY_REQUESTS.getCode(),
                    "SMS code already sent, please wait a minute");
        }
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        redissonClient.getBucket(CODE_KEY_PREFIX + mobile).set(code, CODE_TTL);

        String content = String.format(SMS_TEMPLATE, code);
        NoticeDispatcher dispatcher = noticeDispatcher.getIfAvailable();
        if (dispatcher != null) {
            // Prefer structured params — Aliyun/Tencent require {"code":"123456"}
            // and fall back to regex-extraction otherwise.
            if (dispatcher.dispatchSmsTemplate(mobile, "VERIFY_CODE", Map.of("code", code))) {
                log.info("[auth] SMS code dispatched (template) to {}", mobile);
                return;
            }
            if (dispatcher.dispatchSms(mobile, content, "VERIFY_CODE")) {
                log.info("[auth] SMS code dispatched (rendered) to {}", mobile);
                return;
            }
        }
        log.info("[auth] SMS code (fallback log): mobile={} code={}", mobile, code);
    }

    @Override
    public void verifyLoginCode(String mobile, String code) {
        RBucket<String> bucket = redissonClient.getBucket(CODE_KEY_PREFIX + mobile);
        String expected = bucket.getAndDelete();
        if (expected == null) {
            throw new BizException(ResponseCode.UNAUTHORIZED.getCode(),
                    "SMS code expired, please request a new one");
        }
        if (!expected.equals(code)) {
            throw new BizException(ResponseCode.UNAUTHORIZED.getCode(),
                    "SMS code incorrect");
        }
    }
}
