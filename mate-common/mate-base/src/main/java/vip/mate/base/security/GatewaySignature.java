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
package vip.mate.base.security;

import vip.mate.base.constant.AuthHeaders;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.function.Function;

/**
 * HMAC-SHA256 signature over the gateway → downstream auth-context headers.
 *
 * <p>The gateway holds a shared secret and signs the whole identity-header set
 * plus a timestamp; downstream services recompute with the same secret. An
 * attacker who can reach a service port directly (bypassing the gateway) cannot
 * forge a valid signature for any spoofed {@code X-User-Id} / {@code X-Roles} /
 * … combination, because they lack the secret — closing the "send
 * {@code X-User-Id: 1} straight to the service and impersonate the super-admin"
 * hole.
 *
 * <p>The canonical payload is the values of every header in
 * {@link AuthHeaders#ALL} (in that fixed order; missing → empty string), joined
 * by {@code '\n'}, followed by the timestamp. Signing the FULL set — not just
 * the user id — means tampering with any single identity header invalidates the
 * signature. The timestamp bounds the replay window.
 *
 * <p>Pure JDK, zero framework deps, so both the reactive gateway and the servlet
 * downstreams share ONE implementation (a drifting copy would silently break
 * every cross-service call).
 *
 * @author mateaix
 */
public final class GatewaySignature {

    private static final String HMAC_ALGO = "HmacSHA256";

    private GatewaySignature() {
    }

    /**
     * Canonical signing payload: each {@link AuthHeaders#ALL} value (missing →
     * {@code ""}) followed by {@code '\n'}, then the timestamp string.
     */
    public static String canonicalPayload(Function<String, String> headerLookup, String ts) {
        StringBuilder sb = new StringBuilder(128);
        for (String header : AuthHeaders.ALL) {
            String v = headerLookup.apply(header);
            sb.append(v == null ? "" : v).append('\n');
        }
        return sb.append(ts).toString();
    }

    /** Sign the current header set for the given epoch-millis timestamp. */
    public static String sign(String secret, Function<String, String> headerLookup, long ts) {
        return hmac(secret, canonicalPayload(headerLookup, Long.toString(ts)));
    }

    /**
     * Verify a request's gateway signature.
     *
     * @param secret       shared gateway↔downstream secret
     * @param headerLookup resolves a header name to its value on the request
     * @param tsHeader     value of {@link AuthHeaders#GATEWAY_TS} (epoch millis)
     * @param signHeader   value of {@link AuthHeaders#GATEWAY_SIGN}
     * @param skewMs       max allowed |now - ts| in millis (replay window)
     * @param nowMs        current epoch millis
     * @return true iff the signature is present, fresh, and matches
     */
    public static boolean verify(String secret, Function<String, String> headerLookup,
                                 String tsHeader, String signHeader, long skewMs, long nowMs) {
        if (secret == null || secret.isEmpty()
                || tsHeader == null || tsHeader.isBlank()
                || signHeader == null || signHeader.isBlank()) {
            return false;
        }
        final long ts;
        try {
            ts = Long.parseLong(tsHeader.trim());
        } catch (NumberFormatException e) {
            return false;
        }
        if (Math.abs(nowMs - ts) > skewMs) {
            return false;
        }
        // Recompute with the RAW ts string so no long↔string round-trip can drift.
        String expected = hmac(secret, canonicalPayload(headerLookup, tsHeader));
        // Constant-time compare — never leak how many prefix bytes matched.
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signHeader.getBytes(StandardCharsets.UTF_8));
    }

    private static String hmac(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            // HmacSHA256 is guaranteed by the JDK; an empty secret is caught upstream.
            throw new IllegalStateException("HMAC signing failed", e);
        }
    }
}
