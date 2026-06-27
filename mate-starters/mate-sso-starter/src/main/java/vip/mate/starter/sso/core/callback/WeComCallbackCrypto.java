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
package vip.mate.starter.sso.core.callback;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import vip.mate.base.exception.BizException;
import vip.mate.starter.sso.core.util.Sha1;
import vip.mate.starter.sso.types.SsoErrorCode;

/**
 * WeChat Work (企业微信) callback crypto — the {@code WXBizMsgCrypt} scheme used by
 * the contacts-change callback (and message callbacks).
 *
 * <ul>
 *   <li>Signature = SHA1 of the sorted+joined {@code [token, timestamp, nonce, encrypt]}.</li>
 *   <li>Body = AES-256-CBC, key = Base64({@code encodingAesKey + "="}), IV = key[0..16].</li>
 *   <li>Plaintext = 16B random ‖ 4B msgLen(big-endian) ‖ msg ‖ receiveId, PKCS7-padded.</li>
 * </ul>
 *
 * Stateless; all secrets are passed per call (read from the provider config).
 *
 * @author mateaix
 */
public final class WeComCallbackCrypto {

    private static final Pattern ENCRYPT_TAG =
            Pattern.compile("<Encrypt><!\\[CDATA\\[(.*?)]]></Encrypt>", Pattern.DOTALL);
    private static final Pattern ENCRYPT_TAG_PLAIN =
            Pattern.compile("<Encrypt>(.*?)</Encrypt>", Pattern.DOTALL);

    private WeComCallbackCrypto() {
    }

    /** SHA1 signature over the sorted, concatenated parts. */
    public static String signature(String token, String timestamp, String nonce, String encrypt) {
        String[] arr = {token, timestamp, nonce, encrypt};
        Arrays.sort(arr);
        return Sha1.hex(String.join("", arr));
    }

    /** Constant-time-ish signature check (case-insensitive hex). */
    public static boolean verify(String token, String timestamp, String nonce,
                                 String encrypt, String msgSignature) {
        return msgSignature != null && signature(token, timestamp, nonce, encrypt).equalsIgnoreCase(msgSignature);
    }

    /** Pull the {@code <Encrypt>} payload out of a callback XML body. */
    public static String extractEncrypt(String xmlBody) {
        if (xmlBody == null) {
            return null;
        }
        Matcher m = ENCRYPT_TAG.matcher(xmlBody);
        if (m.find()) {
            return m.group(1).trim();
        }
        m = ENCRYPT_TAG_PLAIN.matcher(xmlBody);
        return m.find() ? m.group(1).trim() : null;
    }

    /**
     * Decrypt an {@code encrypt} blob; returns the inner message (XML for events,
     * or the echo string for URL verification). When {@code expectedReceiveId} is
     * non-blank it must match the trailing receiveId (corpId) or decryption fails.
     */
    public static String decrypt(String encodingAesKey, String encrypt, String expectedReceiveId) {
        try {
            byte[] aesKey = Base64.getDecoder().decode(encodingAesKey + "=");
            byte[] iv = Arrays.copyOfRange(aesKey, 0, 16);
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new IvParameterSpec(iv));
            byte[] plain = pkcs7Unpad(cipher.doFinal(Base64.getDecoder().decode(encrypt)));

            // [0,16) random | [16,20) msgLen(BE) | [20,20+len) msg | [20+len,end) receiveId
            int msgLen = ByteBuffer.wrap(plain, 16, 4).getInt();
            if (msgLen < 0 || 20 + msgLen > plain.length) {
                throw new BizException(SsoErrorCode.SSO_E_HTTP.getCode(), "回调解密长度非法");
            }
            String msg = new String(plain, 20, msgLen, StandardCharsets.UTF_8);
            if (expectedReceiveId != null && !expectedReceiveId.isBlank()) {
                String receiveId = new String(plain, 20 + msgLen, plain.length - 20 - msgLen, StandardCharsets.UTF_8);
                if (!expectedReceiveId.equals(receiveId)) {
                    throw new BizException(SsoErrorCode.SSO_E_HTTP.getCode(), "回调 receiveId 不匹配");
                }
            }
            return msg;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(SsoErrorCode.SSO_E_HTTP.getCode(), "回调解密失败: " + e.getMessage());
        }
    }

    private static byte[] pkcs7Unpad(byte[] data) {
        if (data.length == 0) {
            return data;
        }
        int pad = data[data.length - 1];
        if (pad < 1 || pad > 32) {
            pad = 0;
        }
        return Arrays.copyOfRange(data, 0, data.length - pad);
    }
}
