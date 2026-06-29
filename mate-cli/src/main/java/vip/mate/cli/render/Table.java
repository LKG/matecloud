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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight aligned table renderer with a bold heading, a dim rule and
 * per-cell colouring — a drop-in replacement for the hand-rolled
 * {@code printf("%-24s ...")} blocks scattered across the commands.
 * <p>
 * Column widths are computed from the <em>plain</em> text (styling is applied
 * after padding, so colour codes never break alignment). Over-long cells are
 * truncated with an ellipsis.
 *
 * @author mateaix
 */
public final class Table {

    /** Styles an already-padded cell; must not change its visible width. */
    public interface CellStyler {
        String style(int col, String rawValue, String padded);
    }

    private final String[] headers;
    private final List<String[]> rows = new ArrayList<>();
    private int maxWidth = 60;
    private CellStyler styler;

    private Table(String[] headers) {
        this.headers = headers;
    }

    public static Table of(String... headers) {
        return new Table(headers);
    }

    public Table row(Object... cells) {
        String[] r = new String[headers.length];
        for (int i = 0; i < headers.length; i++) {
            r[i] = i < cells.length && cells[i] != null ? String.valueOf(cells[i]) : "";
        }
        rows.add(r);
        return this;
    }

    public Table maxWidth(int w) {
        this.maxWidth = w;
        return this;
    }

    /** Apply colour to cells by (column, value); width-safe (styles padded text). */
    public Table styler(CellStyler s) {
        this.styler = s;
        return this;
    }

    public void print(PrintStream out) {
        int cols = headers.length;
        int[] width = new int[cols];
        for (int c = 0; c < cols; c++) {
            width[c] = headers[c].length();
        }
        for (String[] r : rows) {
            for (int c = 0; c < cols; c++) {
                width[c] = Math.max(width[c], r[c].length());
            }
        }
        for (int c = 0; c < cols; c++) {
            width[c] = Math.min(width[c], maxWidth);
        }

        // Heading
        StringBuilder head = new StringBuilder();
        int total = 0;
        for (int c = 0; c < cols; c++) {
            head.append(pad(headers[c], width[c]));
            if (c < cols - 1) {
                head.append("  ");
            }
            total += width[c] + (c < cols - 1 ? 2 : 0);
        }
        out.println(Ansi.bold(head.toString()));
        out.println(Ansi.muted("─".repeat(Math.max(total, 1))));

        // Rows
        for (String[] r : rows) {
            StringBuilder line = new StringBuilder();
            for (int c = 0; c < cols; c++) {
                String raw = fit(r[c], width[c]);
                String padded = pad(raw, width[c]);
                if (styler != null) {
                    padded = styler.style(c, r[c], padded);
                }
                line.append(padded);
                if (c < cols - 1) {
                    line.append("  ");
                }
            }
            out.println(line);
        }
    }

    private static String fit(String s, int w) {
        if (s.length() <= w) {
            return s;
        }
        return w <= 1 ? s.substring(0, w) : s.substring(0, w - 1) + "…";
    }

    private static String pad(String s, int w) {
        if (s.length() >= w) {
            return s;
        }
        return s + " ".repeat(w - s.length());
    }
}
