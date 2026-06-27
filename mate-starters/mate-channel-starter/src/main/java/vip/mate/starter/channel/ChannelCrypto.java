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
package vip.mate.starter.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256-GCM encryption for sensitive channel config persisted in
 * {@code mate_channel_config.config_json}.
 *
 * <p>Used symmetrically by the admin write path (mate-system) and the shared read
 * store ({@link DbChannelConfigStore}) so any service decrypts what the admin wrote —
 * the value travels across services, which is why this is a dedicated, key-shared
 * helper rather than a MyBatis {@code TypeHandler} bound to one PO.
 *
 * <p>Format: {@code MGCM1:Base64(IV(12) || ciphertext+tag)}. Values without the
 * prefix are treated as legacy plaintext and returned verbatim (graceful migration).
 *
 * @author mateaix
 */
public class ChannelCrypto {

    private static final Logger log = LoggerFactory.getLogger(ChannelCrypto.class);

    private static final String PREFIX = "MGCM1:";
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final byte[] keyBytes;

    public ChannelCrypto(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                    "Encryption key not configured. Set property: mate.security.encrypt.key");
        }
        byte[] k = key.getBytes(StandardCharsets.UTF_8);
        if (k.length != 16 && k.length != 24 && k.length != 32) {
            throw new IllegalStateException("AES key must be 16/24/32 bytes, got " + k.length);
        }
        this.keyBytes = k;
    }

    /** Encrypt plaintext to {@code MGCM1:...}. Null/blank passes through. */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"),
                    new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ct, 0, combined, iv.length, ct.length);
            return PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            // Fail closed: never silently persist plaintext for a value meant to be encrypted.
            throw new IllegalStateException("Channel config encryption failed", e);
        }
    }

    /** Decrypt {@code MGCM1:...}; legacy plaintext (no prefix) is returned as-is. */
    public String decrypt(String stored) {
        if (stored == null || stored.isEmpty() || !stored.startsWith(PREFIX)) {
            return stored;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            byte[] iv = Arrays.copyOfRange(decoded, 0, IV_LENGTH);
            byte[] ct = Arrays.copyOfRange(decoded, IV_LENGTH, decoded.length);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"),
                    new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("[channel] decrypt failed, returning raw value: {}", e.getMessage());
            return stored;
        }
    }
}
