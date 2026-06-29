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
package vip.mate.cli.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Tiny JDK-HttpClient JSON wrapper used by mate-cli to call service REST
 * endpoints (health, /api/v1/ai/*, /api/v1/ai/tools, ...).
 *
 * @author mateaix
 */
public class JsonHttpClient {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper json = new ObjectMapper();

    public Map<String, Object> get(String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new RuntimeException("HTTP " + resp.statusCode() + ": " + resp.body());
            }
            return json.readValue(resp.body(), new TypeReference<>() { });
        } catch (Exception e) {
            throw new RuntimeException("GET failed: " + url + " — " + e.getMessage(), e);
        }
    }

    public Map<String, Object> postJson(String url, Object body) {
        try {
            byte[] payload = json.writeValueAsBytes(body);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMinutes(3))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new RuntimeException("HTTP " + resp.statusCode() + ": " + resp.body());
            }
            return json.readValue(resp.body(), new TypeReference<>() { });
        } catch (Exception e) {
            throw new RuntimeException("POST failed: " + url + " — " + e.getMessage(), e);
        }
    }

    /**
     * POST a JSON body and consume a Server-Sent-Events stream
     * ({@code text/event-stream}, one {@code data:} line per chunk — Spring's
     * {@code Flux<String>} format). Each chunk is handed to {@code onToken} as
     * it arrives.
     * <p>
     * If the first token does not arrive within {@code firstTokenTimeoutMs} the
     * stream is aborted and {@code false} is returned (no token consumed), so the
     * caller can retry. Returns {@code true} once at least one token was emitted.
     * Throws only if the stream breaks <em>after</em> output already started
     * (a mid-stream failure must not be silently retried — that would duplicate
     * text).
     */
    public boolean postStream(String url, Object body, Consumer<String> onToken, long firstTokenTimeoutMs) {
        ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "mate-sse-watchdog");
            t.setDaemon(true);
            return t;
        });
        Stream<String> lines = null;
        AtomicBoolean firstSeen = new AtomicBoolean(false);
        try {
            byte[] payload = json.writeValueAsBytes(body);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMinutes(5))
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                    .build();
            HttpResponse<Stream<String>> resp = http.send(req, HttpResponse.BodyHandlers.ofLines());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new RuntimeException("HTTP " + resp.statusCode());
            }
            lines = resp.body();
            final Stream<String> ref = lines;
            // First-token watchdog: abort the stream if nothing arrives in time.
            watchdog.schedule(() -> {
                if (!firstSeen.get()) {
                    ref.close();
                }
            }, firstTokenTimeoutMs, TimeUnit.MILLISECONDS);

            Iterator<String> it = lines.iterator();
            boolean any = false;
            while (it.hasNext()) {
                String line = it.next();
                if (line == null || !line.startsWith("data:")) {
                    continue;
                }
                String token = line.substring(5);
                if (token.startsWith(" ")) {
                    token = token.substring(1);
                }
                if (token.isEmpty()) {
                    continue;
                }
                firstSeen.set(true);
                any = true;
                onToken.accept(token);
            }
            return any;
        } catch (Exception e) {
            if (firstSeen.get()) {
                throw new RuntimeException("stream interrupted: " + e.getMessage(), e);
            }
            return false;
        } finally {
            watchdog.shutdownNow();
            if (lines != null) {
                try {
                    lines.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public String tryGetString(String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(3))
                    .header("Accept", "*/*")
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return resp.statusCode() < 200 || resp.statusCode() >= 300 ? null : resp.body();
        } catch (Exception e) {
            return null;
        }
    }
}
