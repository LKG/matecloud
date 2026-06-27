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
package vip.mate.starter.web.trace;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import vip.mate.base.result.Result;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归锁: Boot 4 用 Jackson 3 (tools.jackson) 做 HTTP 序列化, 经
 * {@code Jackson3Configuration} 的 JsonMapperBuilderCustomizer 后, API 响应里的 java.time
 * 必须仍是 matecloud 约定格式 (而非 Jackson 3 默认的 ISO 'T')。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.main.banner-mode=off")
class JacksonHttpSerializationTest {

    @LocalServerPort
    int port;

    @Test
    void javaTimeHttpFormatFollowsMateConvention() throws Exception {
        HttpResponse<String> resp = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/jackson-probe")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        String body = resp.body();

        assertThat(body)
                .as("LocalDateTime 应为 'yyyy-MM-dd HH:mm:ss' (Jackson 3 + Jackson3Configuration), body=%s", body)
                .contains("\"dateTime\":\"2026-01-02 03:04:05\"")
                .doesNotContain("2026-01-02T03:04:05");
        assertThat(body).as("LocalDate 应为 'yyyy-MM-dd'").contains("\"date\":\"2026-01-02\"");
        assertThat(body).as("LocalTime 应为 'HH:mm:ss'").contains("\"time\":\"03:04:05\"");
    }

    @SpringBootApplication
    static class TestApp {
        @Bean
        ProbeController probeController() {
            return new ProbeController();
        }
    }

    @RestController
    static class ProbeController {
        @GetMapping("/jackson-probe")
        public Result<Probe> probe() {
            return Result.ok(new Probe(
                    LocalDateTime.of(2026, 1, 2, 3, 4, 5),
                    LocalDate.of(2026, 1, 2),
                    LocalTime.of(3, 4, 5)));
        }
    }

    public record Probe(LocalDateTime dateTime, LocalDate date, LocalTime time) {
    }
}
