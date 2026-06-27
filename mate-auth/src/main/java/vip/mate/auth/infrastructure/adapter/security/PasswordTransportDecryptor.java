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
package vip.mate.auth.infrastructure.adapter.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vip.mate.base.exception.BizException;
import vip.mate.base.response.ResponseCode;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Decrypts passwords that were AES-CFB encrypted by the frontend before
 * transport. This prevents plain-text passwords from appearing in HTTP
 * request bodies, proxy logs, or WAF audit trails — even when TLS is in
 * place.
 *
 * <p>Algorithm: AES/CFB/NoPadding, 128-bit key.
 * The IV is the same as the key (matching the frontend CryptoJS convention).
 *
 * <p>The 16-char key is configured via {@code mate.security.transport-key}
 * and MUST match the frontend's {@code DEFAULT_TRANSPORT_KEY} in
 * {@code crypto.ts}.
 *
 * @author MateCloud
 *
 * @author mateaix
 */
@Slf4j
@Component
public class PasswordTransportDecryptor {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CFB/NoPadding";

    private final byte[] keyBytes;

    public PasswordTransportDecryptor(
            @Value("${mate.security.transport-key:mate9x!K#2qL8pZw}") String transportKey) {
        if (transportKey.length() != 16) {
            throw new IllegalArgumentException(
                    "mate.security.transport-key must be exactly 16 characters (128-bit AES)");
        }
        this.keyBytes = transportKey.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Decrypt a Base64-encoded AES-CFB ciphertext back to the original
     * plain-text password.
     *
     * @param encryptedBase64 Base64 string produced by the frontend's
     *                        {@code encryptPassword()}
     * @return the original plain-text password
     * @throws BizException if decryption fails (tampered data, wrong key, etc.)
     */
    public String decrypt(String encryptedBase64) {
        if (encryptedBase64 == null || encryptedBase64.isBlank()) {
            throw new BizException(ResponseCode.BAD_REQUEST.getCode(),
                    "Encrypted password is required");
        }
        try {
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
            IvParameterSpec iv = new IvParameterSpec(keyBytes);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);

            byte[] decoded = Base64.getDecoder().decode(encryptedBase64);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("[auth] Password transport decryption failed: {}", e.getMessage());
            throw new BizException(ResponseCode.BAD_REQUEST.getCode(),
                    "Invalid encrypted password");
        }
    }
}
