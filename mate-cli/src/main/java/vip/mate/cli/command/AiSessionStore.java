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
package vip.mate.cli.command;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Local persistence for {@code ai chat} conversations. The multi-turn memory
 * itself lives server-side (Spring AI, keyed by {@code conversationId}); this
 * store keeps a local record of that id plus the transcript so the CLI can list,
 * resume, show and fork conversations across invocations.
 *
 * <p>One JSON file per session under {@code ~/.mate/ai-sessions/<id>.json}.
 *
 * @author mateaix
 */
public final class AiSessionStore {

    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private AiSessionStore() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Session {
        public String id;
        public String title;
        public String created;
        public String updated;
        public List<Turn> turns = new ArrayList<>();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Turn {
        public String role;   // "user" | "assistant"
        public String text;
        public String ts;

        public Turn() {
        }

        public Turn(String role, String text) {
            this.role = role;
            this.text = text;
            this.ts = now();
        }
    }

    // ---- lifecycle ----

    public static String newId() {
        return "mate-cli-" + UUID.randomUUID();
    }

    /** Load a session by id, or create a fresh (unsaved) one carrying that id. */
    public static Session loadOrCreate(String id) {
        Session s = load(id);
        if (s != null) {
            return s;
        }
        s = new Session();
        s.id = id;
        s.created = now();
        return s;
    }

    public static Session load(String id) {
        File f = fileFor(id);
        if (!f.isFile()) {
            return null;
        }
        try {
            return JSON.readValue(f, Session.class);
        } catch (Exception e) {
            return null;
        }
    }

    /** Append a user+assistant exchange and persist. */
    public static void record(Session s, String user, String assistant) {
        if (s.title == null || s.title.isBlank()) {
            s.title = user.length() > 60 ? user.substring(0, 57) + "…" : user;
        }
        s.turns.add(new Turn("user", user));
        s.turns.add(new Turn("assistant", assistant));
        s.updated = now();
        try {
            JSON.writeValue(fileFor(s.id), s);
        } catch (Exception ignored) {
            // Persistence is best-effort; a failed save must not break the chat.
        }
    }

    public static List<Session> list() {
        List<Session> all = new ArrayList<>();
        File[] files = dir().listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) {
            return all;
        }
        for (File f : files) {
            try {
                all.add(JSON.readValue(f, Session.class));
            } catch (Exception ignored) {
                // skip corrupt files
            }
        }
        all.sort(Comparator.comparing((Session s) -> s.updated == null ? "" : s.updated).reversed());
        return all;
    }

    /** Copy a session's local transcript under a new id (a local branch). */
    public static Session fork(String srcId, String newId) {
        Session src = load(srcId);
        if (src == null) {
            return null;
        }
        Session copy = new Session();
        copy.id = newId;
        copy.title = (src.title == null ? "" : src.title) + " (fork)";
        copy.created = now();
        copy.updated = now();
        copy.turns = new ArrayList<>(src.turns);
        try {
            JSON.writeValue(fileFor(copy.id), copy);
        } catch (Exception e) {
            return null;
        }
        return copy;
    }

    // ---- helpers ----

    private static File dir() {
        File d = new File(System.getProperty("user.home"), ".mate/ai-sessions");
        //noinspection ResultOfMethodCallIgnored
        d.mkdirs();
        return d;
    }

    private static File fileFor(String id) {
        String safe = id.replaceAll("[^a-zA-Z0-9._-]", "_");
        return new File(dir(), safe + ".json");
    }

    private static String now() {
        return OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
