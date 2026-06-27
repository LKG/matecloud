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
package vip.mate.starter.sms.model;

/**
 * Outcome of an SMS send.
 *
 * @param success      whether the gateway acknowledged the send
 * @param providerType the provider that handled it (e.g. {@code "aliyun"})
 * @param bizId        gateway-assigned id, if any
 * @param message      failure reason / gateway message, if any
 *
 * @author mateaix
 */
public record SmsResult(boolean success, String providerType, String bizId, String message) {

    public static SmsResult ok(String providerType, String bizId) {
        return new SmsResult(true, providerType, bizId, null);
    }

    public static SmsResult fail(String providerType, String message) {
        return new SmsResult(false, providerType, null, message);
    }
}
