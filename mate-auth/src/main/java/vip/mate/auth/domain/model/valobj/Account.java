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
package vip.mate.auth.domain.model.valobj;

import java.util.regex.Pattern;

/**
 * An {@code Account} is the string the user typed into the login form. It may
 * be a username, a mobile number, or an email address. This value object
 * classifies it at construction time so downstream code can route to the
 * correct repository query.
 *
 * @author mateaix
 */
public record Account(String value, Kind kind) {

    private static final Pattern MOBILE_RE = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL_RE = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    public enum Kind { USERNAME, MOBILE, EMAIL }

    public Account {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Account value must not be blank");
        }
        if (kind == null) {
            throw new IllegalArgumentException("Account kind must not be null");
        }
    }

    /**
     * Classify a raw account string. Mobile regex first (strictest), then email,
     * then fall back to username.
     */
    public static Account of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Account must not be blank");
        }
        String trimmed = raw.trim();
        if (MOBILE_RE.matcher(trimmed).matches()) {
            return new Account(trimmed, Kind.MOBILE);
        }
        if (EMAIL_RE.matcher(trimmed).matches()) {
            return new Account(trimmed, Kind.EMAIL);
        }
        return new Account(trimmed, Kind.USERNAME);
    }

    /** Shortcut constructor for known-type callers. */
    public static Account mobile(String mobile) {
        return new Account(mobile, Kind.MOBILE);
    }

    public boolean isMobile()   { return kind == Kind.MOBILE; }
    public boolean isEmail()    { return kind == Kind.EMAIL; }
    public boolean isUsername() { return kind == Kind.USERNAME; }
}
