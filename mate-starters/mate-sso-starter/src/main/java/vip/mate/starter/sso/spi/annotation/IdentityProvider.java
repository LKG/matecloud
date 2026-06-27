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
package vip.mate.starter.sso.spi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an identity-provider implementation and declares the provider code it
 * serves (e.g. {@code "wechat_work"}, {@code "dingtalk"}, {@code "feishu"},
 * {@code "ldap"}).
 *
 * <p>Mirrors {@code @FileProvider} of mate-file-starter (a pure marker, not a
 * {@code @Component}): the bundled providers are registered as {@code @Bean}s in
 * {@code SsoAutoConfiguration}, and the {@code ProviderRegistry} keys them by this
 * value — so adding a channel is just "implement the SPI + annotate + register a
 * bean", no central switch.
 *
 * @see vip.mate.starter.sso.spi.IdentityProvider
 *
 * @author mateaix
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface IdentityProvider {

    /** Provider code key, e.g. {@code "wechat_work"}. */
    String value();

    /** Human-readable description (optional). */
    String describe() default "";
}
