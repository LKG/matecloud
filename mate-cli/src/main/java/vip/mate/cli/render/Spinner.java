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

/**
 * Tiny Braille spinner rendered on stderr (so it never pollutes piped stdout).
 * Animates only on a real TTY; on a non-TTY it prints the label once. Designed
 * for "waiting for first token" during streaming.
 *
 * @author mateaix
 */
public final class Spinner {

    private static final String[] FRAMES = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};

    private final String label;
    private final PrintStream err = System.err;
    private volatile boolean running;
    private Thread thread;

    public Spinner(String label) {
        this.label = label;
    }

    public void start() {
        if (!Ansi.enabled()) {
            err.println(label + " …");
            return;
        }
        running = true;
        thread = new Thread(() -> {
            int i = 0;
            while (running) {
                err.print("\r" + Ansi.cyan(FRAMES[i++ % FRAMES.length]) + " " + Ansi.muted(label));
                err.flush();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "mate-spinner");
        thread.setDaemon(true);
        thread.start();
    }

    /** Stop the animation and erase the spinner line. */
    public void stop() {
        running = false;
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join(200);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        if (Ansi.enabled()) {
            err.print("\r" + " ".repeat(label.length() + 4) + "\r");
            err.flush();
        }
    }
}
