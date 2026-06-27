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
package vip.mate.starter.sso.core.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import vip.mate.starter.sso.types.SsoErrorCode;
import vip.mate.base.exception.BizException;

/**
 * {@link HttpExecutor} on the JDK {@link HttpClient} (no third-party HTTP dep).
 *
 * <p>GET is idempotent, so it is retried a few times on transient failures
 * (network {@link IOException}, HTTP 429/5xx) with linear back-off — this keeps a
 * full contacts sync from failing wholesale on a single network blip. POST is not
 * retried (vendor write/login calls are not assumed idempotent).
 *
 * @author mateaix
 */
public class JdkHttpExecutor implements HttpExecutor {

    private static final int GET_MAX_ATTEMPTS = 3;
    private static final long RETRY_BASE_BACKOFF_MS = 200L;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper mapper;

    public JdkHttpExecutor(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Map<String, Object> getJson(String url) {
        return getJson(url, Map.of());
    }

    @Override
    public Map<String, Object> getJson(String url, Map<String, String> headers) {
        HttpRequest req = withHeaders(HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET(), headers).build();
        return sendWithRetry(req);
    }

    @Override
    public Map<String, Object> postJson(String url, Object body) {
        return postJson(url, body, Map.of());
    }

    @Override
    public Map<String, Object> postJson(String url, Object body, Map<String, String> headers) {
        try {
            String json = mapper.writeValueAsString(body);
            HttpRequest req = withHeaders(HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)), headers).build();
            return send(req); // POST: single attempt (not assumed idempotent)
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(SsoErrorCode.SSO_E_HTTP.getCode(), "SSO HTTP POST failed: " + e.getMessage());
        }
    }

    private static HttpRequest.Builder withHeaders(HttpRequest.Builder builder, Map<String, String> headers) {
        if (headers != null) {
            headers.forEach(builder::header);
        }
        return builder;
    }

    /** Idempotent send with linear back-off on transient failures. */
    private Map<String, Object> sendWithRetry(HttpRequest req) {
        BizException last = null;
        for (int attempt = 1; attempt <= GET_MAX_ATTEMPTS; attempt++) {
            try {
                return send(req);
            } catch (TransientHttpException e) {
                last = new BizException(SsoErrorCode.SSO_E_HTTP.getCode(), e.getMessage());
                if (attempt < GET_MAX_ATTEMPTS) {
                    sleep(attempt * RETRY_BASE_BACKOFF_MS);
                }
            }
        }
        throw last;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> send(HttpRequest req) {
        try {
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int sc = resp.statusCode();
            if (sc / 100 == 2) {
                return mapper.readValue(resp.body(), Map.class);
            }
            String msg = "SSO HTTP " + sc + ": " + resp.body();
            if (sc == 429 || sc >= 500) {
                throw new TransientHttpException(msg); // worth retrying for GET
            }
            throw new BizException(SsoErrorCode.SSO_E_HTTP.getCode(), msg);
        } catch (IOException e) {
            throw new TransientHttpException("SSO HTTP call failed: " + e.getMessage()); // network blip
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(SsoErrorCode.SSO_E_HTTP.getCode(), "SSO HTTP call interrupted");
        } catch (BizException | TransientHttpException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(SsoErrorCode.SSO_E_HTTP.getCode(), "SSO HTTP parse failed: " + e.getMessage());
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Internal marker for failures a GET may retry; never escapes this class. */
    private static final class TransientHttpException extends RuntimeException {
        TransientHttpException(String message) {
            super(message);
        }
    }
}
