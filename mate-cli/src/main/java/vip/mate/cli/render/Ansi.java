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
package vip.mate.cli.render;

/**
 * Minimal ANSI styling for terminal output — no external dependency (keeps
 * mate-cli framework-free). Colour is auto-disabled when output is piped or
 * {@code NO_COLOR} is set, and can be forced with {@code MATE_COLOR=always|never}.
 * <p>
 * Styles are composed as a single SGR sequence + one reset (e.g. {@code heading}
 * is {@code ESC[1;36m…ESC[0m}), so nesting never emits doubled reset codes.
 *
 * @author mateaix
 */
public final class Ansi {

    private static final char ESC = '';
    private static final String RESET = ESC + "[0m";
    private static final boolean ENABLED = resolve();

    private Ansi() {
    }

    private static boolean resolve() {
        String force = System.getenv("MATE_COLOR");
        if ("never".equalsIgnoreCase(force)) {
            return false;
        }
        if ("always".equalsIgnoreCase(force)) {
            return true;
        }
        // NO_COLOR convention: any value disables colour.
        if (System.getenv("NO_COLOR") != null) {
            return false;
        }
        // System.console() is null when stdout is piped/redirected → no colour.
        return System.console() != null;
    }

    /** Whether ANSI styling is active (also gates spinner animation). */
    public static boolean enabled() {
        return ENABLED;
    }

    /** Wrap {@code s} in one SGR sequence ({@code params}, e.g. "1;36") + reset. */
    private static String sgr(String params, String s) {
        return ENABLED ? ESC + "[" + params + "m" + s + RESET : s;
    }

    public static String bold(String s)     { return sgr("1", s); }
    public static String dim(String s)      { return sgr("2", s); }
    public static String red(String s)      { return sgr("31", s); }
    public static String green(String s)    { return sgr("32", s); }
    public static String yellow(String s)   { return sgr("33", s); }
    public static String blue(String s)     { return sgr("34", s); }
    public static String magenta(String s)  { return sgr("35", s); }
    public static String cyan(String s)     { return sgr("36", s); }

    // ---- Semantic helpers ----

    /** Section / table title (bold cyan, single sequence). */
    public static String heading(String s) { return sgr("1;36", s); }

    /** Secondary / structural text (rules, hints). */
    public static String muted(String s)   { return dim(s); }

    public static String ok(String s)       { return green(s); }
    public static String warn(String s)     { return yellow(s); }
    public static String fail(String s)     { return red(s); }

    /** Colour a health/status token by its conventional meaning. */
    public static String status(String s) {
        return statusColor(s, s);
    }

    /**
     * Colour {@code text} based on the meaning of {@code key} — lets a table
     * colour an already-padded cell ({@code text}) by its raw value ({@code key}),
     * keeping column width intact.
     */
    public static String statusColor(String key, String text) {
        if (key == null) {
            return text;
        }
        return switch (key.trim().toUpperCase()) {
            case "UP", "OK", "HEALTHY", "TRUE" -> green(text);
            case "DOWN", "FAIL", "ERROR", "FALSE" -> red(text);
            case "UNREACHABLE", "UNKNOWN", "-", "STALLED", "NONE", "(NONE)" -> yellow(text);
            default -> text;
        };
    }
}
