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
package vip.mate.starter.idempotent.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as idempotent: concurrent/duplicate calls sharing the same
 * resolved key within {@link #expireSeconds()} are rejected.
 *
 * <p><b>Failure semantics — read before relying on this.</b> The idempotency key
 * is <i>released</i> if the annotated method throws, so a failed call can be
 * safely retried. The flip side: if the method has already produced a side effect
 * (e.g. charged a card, sent a message) and then throws, the retry will run the
 * method again. Idempotency therefore guarantees "at most one <i>successful</i>
 * execution" only when the method's side effects are themselves transactional or
 * compensatable. For non-retryable side effects, make the side effect idempotent
 * at its own layer (e.g. a unique constraint) rather than depending on this key.
 *
 * @author mateaix
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    String key() default "";

    long expireSeconds() default 86400;

    String message() default "Duplicate request rejected";

    IdempotentType type() default IdempotentType.PARAM;
}
