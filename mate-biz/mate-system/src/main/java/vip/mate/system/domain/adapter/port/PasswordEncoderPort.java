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
package vip.mate.system.domain.adapter.port;

/**
 * Domain port for password hashing/verification.
 * <p>
 * Keeps the domain free of any concrete crypto framework (e.g. Spring Security's
 * {@code BCryptPasswordEncoder}) — the domain only depends on this abstraction,
 * the implementation lives in {@code infrastructure}.
 *
 * @author mateaix
 */
public interface PasswordEncoderPort {

    /** Hash a raw password for storage. */
    String encode(String rawPassword);

    /** Verify a raw password against a previously {@link #encode(String) encoded} hash. */
    boolean matches(String rawPassword, String encodedPassword);
}
