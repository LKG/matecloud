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
package vip.mate.starter.sms.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import vip.mate.starter.sms.core.SmsProviderResolver;
import vip.mate.starter.sms.core.SmsSenderRegistry;
import vip.mate.starter.sms.spi.SmsSender;

/**
 * Marks a {@link SmsSender} implementation and declares the provider type it
 * serves (e.g. {@code "log"}, {@code "aliyun"}, {@code "tencent"}).
 *
 * <p>{@link SmsSenderRegistry} keys senders by this value; {@code mate.sms.provider}
 * (via {@link SmsProviderResolver}) selects which one is active. Adding a gateway
 * is just: implement {@link SmsSender}, annotate, register as a bean.
 *
 * @author mateaix
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SmsProvider {

    /** Provider type key, e.g. {@code "aliyun"}. */
    String value();

    /** Human-readable description (optional). */
    String describe() default "";
}
