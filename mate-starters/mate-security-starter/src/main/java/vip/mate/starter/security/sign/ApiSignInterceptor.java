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
package vip.mate.starter.security.sign;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import vip.mate.base.exception.BizException;
import vip.mate.starter.security.SecurityErrorCode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Interceptor that verifies API signatures for methods annotated with {@link ApiSign}.
 *
 * <p>Expected headers: X-App-Key, X-Timestamp, X-Nonce, X-Sign.
 * <p>Signature: HMAC-SHA256(appSecret, appKey + timestamp + nonce + body)
 *
 * @author mateaix
 */
@Slf4j
@RequiredArgsConstructor
public class ApiSignInterceptor implements HandlerInterceptor {

    private static final String HEADER_APP_KEY = "X-App-Key";
    private static final String HEADER_TIMESTAMP = "X-Timestamp";
    private static final String HEADER_NONCE = "X-Nonce";
    private static final String HEADER_SIGN = "X-Sign";
    private static final String NONCE_KEY_PREFIX = "api:sign:nonce:";

    private final AppKeyProvider appKeyProvider;
    private final RedissonClient redissonClient;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        ApiSign apiSign = handlerMethod.getMethodAnnotation(ApiSign.class);
        if (apiSign == null) {
            return true;
        }

        String appKey = request.getHeader(HEADER_APP_KEY);
        String timestamp = request.getHeader(HEADER_TIMESTAMP);
        String nonce = request.getHeader(HEADER_NONCE);
        String sign = request.getHeader(HEADER_SIGN);

        if (appKey == null || timestamp == null || nonce == null || sign == null) {
            throw new BizException(SecurityErrorCode.SIGN_INVALID);
        }

        // Validate timestamp within allowed timeout (default 5 minutes = 300 seconds)
        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            throw new BizException(SecurityErrorCode.SIGN_INVALID);
        }

        long now = System.currentTimeMillis() / 1000;
        long timeout = apiSign.timeout();
        if (Math.abs(now - ts) > timeout) {
            throw new BizException(SecurityErrorCode.SIGN_EXPIRED);
        }

        // Validate nonce not replayed (Redis SETNX with TTL). The timestamp check
        // above accepts |now - ts| <= timeout, i.e. a window 2*timeout wide, so the
        // nonce must be retained for the FULL window (2*timeout) — otherwise a
        // captured request could be replayed after the nonce expired but while its
        // timestamp is still within range.
        String nonceKey = NONCE_KEY_PREFIX + nonce;
        boolean isNew = redissonClient.<Integer>getBucket(nonceKey)
                .trySet(1, timeout * 2, TimeUnit.SECONDS);
        if (!isNew) {
            throw new BizException(SecurityErrorCode.REPLAY_ATTACK);
        }

        // Look up app secret
        String appSecret = appKeyProvider.getSecret(appKey);
        if (appSecret == null || appSecret.isBlank()) {
            throw new BizException(SecurityErrorCode.APP_KEY_NOT_FOUND);
        }

        // Read request body. Only application/json bodies are cached (by
        // CachedBodyFilter) and thus available to include in the signature. If a
        // request carries a non-empty body that wasn't cached (e.g. form/multipart/
        // text), we cannot bind it into the signature — fail closed rather than
        // silently signing an empty body, which would leave the body tamperable.
        String body = "";
        if (request instanceof CachedBodyRequestWrapper wrapper) {
            body = new String(wrapper.getCachedBody(), StandardCharsets.UTF_8);
        } else if (request.getContentLengthLong() > 0) {
            log.warn("API sign: request body (content-type={}) is not integrity-protected for uri={}; "
                            + "@ApiSign only covers application/json bodies",
                    request.getContentType(), request.getRequestURI());
            throw new BizException(SecurityErrorCode.SIGN_INVALID);
        }

        // Compute HMAC-SHA256
        String data = appKey + timestamp + nonce + body;
        String computedSign = hmacSha256(appSecret, data);

        if (!constantTimeEqualsIgnoreCase(computedSign, sign)) {
            log.warn("API sign mismatch for appKey={}, uri={}", appKey, request.getRequestURI());
            throw new BizException(SecurityErrorCode.SIGN_INVALID);
        }

        return true;
    }

    private String hmacSha256(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new BizException(SecurityErrorCode.SIGN_INVALID);
        }
    }

    /**
     * Constant-time, case-insensitive comparison of two hex signatures. Avoids the
     * early-exit timing side-channel of {@code String.equalsIgnoreCase} when
     * verifying an attacker-supplied signature.
     */
    private static boolean constantTimeEqualsIgnoreCase(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        byte[] a = expected.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
        byte[] b = actual.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }
}
