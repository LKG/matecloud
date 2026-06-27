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
package vip.mate.ai.infrastructure.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM cipher used to encrypt the API keys stored in
 * {@code mate_ai_provider.api_key_cipher}. The plain key is NEVER returned
 * from any API — callers see only a redacted preview.
 *
 * <p>The encryption key comes from {@code mate.ai.encryption.key} (in Nacos
 * for prod). When unset, we derive a dev-only key from a fixed string and
 * log a loud warning at startup so it cannot silently ship to production.
 *
 * <p>Storage format: {@code base64( iv[12] || ciphertext || tag[16] )}
 * with a fixed prefix {@code "v1:"} for future algorithm migration.
 *
 * @author mateaix
 */
@Slf4j
@Component
public class ApiKeyCipher {

    private static final String CIPHER_ALGO = "AES/GCM/NoPadding";
    private static final int GCM_IV_LEN = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final String FORMAT_PREFIX = "v1:";

    private final SecretKeySpec keySpec;
    private final SecureRandom random = new SecureRandom();

    public ApiKeyCipher(@Value("${mate.ai.encryption.key:}") String configuredKey) {
        String src = configuredKey;
        if (src == null || src.isBlank()) {
            log.warn("[mate-ai] mate.ai.encryption.key is unset — using INSECURE dev fallback. " +
                     "Configure a 32-byte secret in Nacos before going to production.");
            src = "mate-ai-default-dev-key-do-not-use-in-prod";
        }
        this.keySpec = new SecretKeySpec(sha256(src), "AES");
    }

    /** Encrypt a plain API key. Returns null when input is null/blank. */
    public String encrypt(String plain) {
        if (plain == null || plain.isBlank()) return null;
        try {
            byte[] iv = new byte[GCM_IV_LEN];
            random.nextBytes(iv);
            Cipher c = Cipher.getInstance(CIPHER_ALGO);
            c.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ct = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buf = ByteBuffer.allocate(iv.length + ct.length).put(iv).put(ct);
            return FORMAT_PREFIX + Base64.getEncoder().encodeToString(buf.array());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt API key", e);
        }
    }

    /** Decrypt back to plaintext. Returns null when input is null/blank. */
    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isBlank()) return null;
        if (!cipherText.startsWith(FORMAT_PREFIX)) {
            // Legacy / corrupt — be loud but don't crash.
            log.warn("[mate-ai] API key has unknown format, leaving as-is");
            return null;
        }
        try {
            byte[] all = Base64.getDecoder().decode(cipherText.substring(FORMAT_PREFIX.length()));
            ByteBuffer buf = ByteBuffer.wrap(all);
            byte[] iv = new byte[GCM_IV_LEN];
            buf.get(iv);
            byte[] ct = new byte[buf.remaining()];
            buf.get(ct);
            Cipher c = Cipher.getInstance(CIPHER_ALGO);
            c.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(c.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt API key", e);
        }
    }

    /** Mask preview for safe display (e.g. {@code sk-***...xyz}). */
    public String preview(String cipherText) {
        if (cipherText == null || cipherText.isBlank()) return "";
        String plain;
        try {
            plain = decrypt(cipherText);
        } catch (Exception e) {
            return "****";
        }
        if (plain == null || plain.length() <= 4) return "****";
        int prefixLen = Math.min(4, plain.length() / 4);
        int suffixLen = Math.min(4, plain.length() / 4);
        return plain.substring(0, prefixLen) + "***" + plain.substring(plain.length() - suffixLen);
    }

    private static byte[] sha256(String s) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(s.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
