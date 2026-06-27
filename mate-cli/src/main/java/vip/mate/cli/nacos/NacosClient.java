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
package vip.mate.cli.nacos;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import vip.mate.cli.config.CliConfig;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal Nacos Open API v2 HTTP client, JDK-only (no SDK jar needed).
 * <p>
 * Supports the calls mate-cli needs: service/instance listing, config get/put/
 * delete, and a login bootstrap when Nacos auth is enabled.
 *
 * @author mateaix
 */
public class NacosClient {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper json = new ObjectMapper();
    private final String serverAddr;
    private final String namespace;
    private final String group;
    private final String username;
    private final String password;
    private volatile String accessToken;

    public NacosClient() {
        this.serverAddr = normalize(CliConfig.nacosServerAddr());
        this.namespace = CliConfig.nacosNamespace();
        this.group = CliConfig.nacosGroup();
        this.username = CliConfig.nacosUsername();
        this.password = CliConfig.nacosPassword();
    }

    public String getServerAddr() { return serverAddr; }
    public String getNamespace()  { return namespace; }
    public String getGroup()      { return group; }

    // -----------------------------------------------------------------
    // Service discovery
    // -----------------------------------------------------------------

    public List<String> listServices() {
        String url = serverAddr + "/nacos/v1/ns/service/list?pageNo=1&pageSize=500"
                + "&namespaceId=" + enc(namespace)
                + "&groupName=" + enc(group)
                + tokenParam();
        String body = get(url);
        try {
            Map<String, Object> parsed = json.readValue(body, new TypeReference<>() { });
            Object names = parsed.get("doms");
            if (names instanceof List<?> list) {
                List<String> out = new ArrayList<>(list.size());
                for (Object n : list) out.add(String.valueOf(n));
                return out;
            }
            return List.of();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse service list: " + body, e);
        }
    }

    public List<Map<String, Object>> listInstances(String serviceName) {
        String url = serverAddr + "/nacos/v1/ns/instance/list?serviceName=" + enc(serviceName)
                + "&namespaceId=" + enc(namespace)
                + "&groupName=" + enc(group)
                + tokenParam();
        String body = get(url);
        try {
            Map<String, Object> parsed = json.readValue(body, new TypeReference<>() { });
            Object hosts = parsed.get("hosts");
            if (hosts instanceof List<?> list) {
                List<Map<String, Object>> out = new ArrayList<>(list.size());
                for (Object h : list) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> m = (Map<String, Object>) h;
                    out.add(m);
                }
                return out;
            }
            return List.of();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse instance list: " + body, e);
        }
    }

    // -----------------------------------------------------------------
    // Config center
    // -----------------------------------------------------------------

    public String getConfig(String dataId, String groupOverride) {
        String g = (groupOverride == null || groupOverride.isBlank()) ? group : groupOverride;
        String url = serverAddr + "/nacos/v1/cs/configs?dataId=" + enc(dataId)
                + "&group=" + enc(g)
                + "&tenant=" + enc(namespace)
                + tokenParam();
        return get(url);
    }

    public boolean publishConfig(String dataId, String groupOverride, String content) {
        String g = (groupOverride == null || groupOverride.isBlank()) ? group : groupOverride;
        Map<String, String> form = new LinkedHashMap<>();
        form.put("dataId", dataId);
        form.put("group", g);
        form.put("tenant", namespace);
        form.put("content", content);
        form.put("type", "yaml");
        String url = serverAddr + "/nacos/v1/cs/configs" + tokenParam();
        String body = postForm(url, form);
        return "true".equalsIgnoreCase(body.trim());
    }

    public boolean deleteConfig(String dataId, String groupOverride) {
        String g = (groupOverride == null || groupOverride.isBlank()) ? group : groupOverride;
        String url = serverAddr + "/nacos/v1/cs/configs?dataId=" + enc(dataId)
                + "&group=" + enc(g)
                + "&tenant=" + enc(namespace)
                + tokenParam();
        String body = delete(url);
        return "true".equalsIgnoreCase(body.trim());
    }

    // -----------------------------------------------------------------
    // Internal HTTP helpers
    // -----------------------------------------------------------------

    private String tokenParam() {
        ensureToken();
        return accessToken == null ? "" : "&accessToken=" + accessToken;
    }

    private synchronized void ensureToken() {
        if (accessToken != null) return;
        if (username == null || username.isBlank()) return;
        String loginUrl = serverAddr + "/nacos/v1/auth/login";
        Map<String, String> form = new LinkedHashMap<>();
        form.put("username", username);
        form.put("password", password);
        try {
            String resp = postForm(loginUrl, form);
            Map<String, Object> parsed = json.readValue(resp, new TypeReference<>() { });
            Object t = parsed.get("accessToken");
            if (t != null) accessToken = String.valueOf(t);
        } catch (Exception ignored) {
            // Nacos may not have auth enabled — proceed without token
        }
    }

    private String get(String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            checkStatus(resp);
            return resp.body();
        } catch (Exception e) {
            throw new RuntimeException("Nacos GET failed: " + url + " — " + e.getMessage(), e);
        }
    }

    private String postForm(String url, Map<String, String> form) {
        try {
            String body = form.entrySet().stream()
                    .map(e -> enc(e.getKey()) + "=" + enc(e.getValue()))
                    .reduce((a, b) -> a + "&" + b)
                    .orElse("");
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            checkStatus(resp);
            return resp.body();
        } catch (Exception e) {
            throw new RuntimeException("Nacos POST failed: " + url + " — " + e.getMessage(), e);
        }
    }

    private String delete(String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .DELETE()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            checkStatus(resp);
            return resp.body();
        } catch (Exception e) {
            throw new RuntimeException("Nacos DELETE failed: " + url + " — " + e.getMessage(), e);
        }
    }

    private void checkStatus(HttpResponse<String> resp) {
        int s = resp.statusCode();
        if (s < 200 || s >= 300) {
            throw new RuntimeException("Nacos HTTP " + s + ": " + resp.body());
        }
    }

    private static String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String normalize(String addr) {
        if (addr == null || addr.isBlank()) return "http://127.0.0.1:8848";
        if (addr.startsWith("http://") || addr.startsWith("https://")) return addr.replaceAll("/+$", "");
        return "http://" + addr;
    }
}
