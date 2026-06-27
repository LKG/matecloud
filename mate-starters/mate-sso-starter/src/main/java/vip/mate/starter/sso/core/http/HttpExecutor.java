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

import java.util.Map;

/**
 * Minimal HTTP port used by OAuth providers, so providers stay testable and free
 * of any specific HTTP-client dependency. Default impl: {@link JdkHttpExecutor}.
 *
 * <p>GET is treated as idempotent and retried on transient failures; POST is not.
 * Header-aware overloads exist for providers that authenticate via a request
 * header (e.g. Feishu's {@code Authorization: Bearer}) rather than a query param.
 *
 * @author mateaix
 */
public interface HttpExecutor {

    /** HTTP GET, returns the response body parsed as a JSON map. */
    Map<String, Object> getJson(String url);

    /** HTTP GET with extra request headers. */
    Map<String, Object> getJson(String url, Map<String, String> headers);

    /** HTTP POST with a JSON body, returns the response body parsed as a JSON map. */
    Map<String, Object> postJson(String url, Object body);

    /** HTTP POST with a JSON body and extra request headers. */
    Map<String, Object> postJson(String url, Object body, Map<String, String> headers);
}
