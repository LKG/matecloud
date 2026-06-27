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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 真·日志验证: 启动一个最小但<b>真实</b>的 Web 上下文(带 OTel 追踪桥), 打一发真实 HTTP 请求,
 * 捕获业务代码<b>实际打出的日志事件</b>, 断言:
 * <ol>
 *   <li>日志行的 MDC 里 {@code traceId} 非空 —— 即「业务日志带上了 traceId」;</li>
 *   <li>该 traceId 同时出现在响应头 {@code X-Trace-Id} 与统一返回体 {@code Result.traceId};</li>
 *   <li>三处<b>完全一致</b> —— 证明日志、响应头、响应体说的是同一条链路。</li>
 * </ol>
 *
 * 这就是「通过日志能否验证」的直接答案: 不依赖任何外部基础设施, 跑真实请求看真实日志。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "management.tracing.sampling.probability=1.0",
                "spring.main.banner-mode=off",
                // 用与 mate-defaults.yml 相同的 pattern, 让控制台日志行<b>肉眼可见</b> traceId
                "logging.pattern.console=%d{HH:mm:ss.SSS} %-5level [%X{traceId},%X{spanId}] %logger{20} - %msg%n"
        })
class TraceLoggingIntegrationTest {

    private static final Pattern TRACE_ID_IN_BODY = Pattern.compile("\"traceId\"\\s*:\\s*\"([^\"]+)\"");

    @LocalServerPort
    int port;

    private ListAppender<ILoggingEvent> logCapture;

    @BeforeEach
    void attachLogCapture() {
        logCapture = new ListAppender<>();
        logCapture.start();
        ((Logger) LoggerFactory.getLogger(TraceTestController.class)).addAppender(logCapture);
    }

    @Test
    void businessLogCarriesTraceId_andItMatchesHeaderAndBody() throws Exception {
        HttpResponse<String> resp = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/trace-probe")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        // (2) 响应头 X-Trace-Id
        String headerTraceId = resp.headers().firstValue("X-Trace-Id").orElse(null);
        assertThat(headerTraceId).as("响应头 X-Trace-Id").isNotBlank();

        // (2) 响应体 Result.traceId
        Matcher m = TRACE_ID_IN_BODY.matcher(resp.body());
        assertThat(m.find()).as("响应体应含 traceId 字段, body=%s", resp.body()).isTrue();
        String bodyTraceId = m.group(1);

        // (1) 业务日志行的 MDC traceId —— 这就是「日志验证」
        List<ILoggingEvent> events = logCapture.list;
        ILoggingEvent businessLog = events.stream()
                .filter(e -> e.getFormattedMessage().contains("handling /trace-probe"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("没捕获到业务日志事件"));
        String logTraceId = businessLog.getMDCPropertyMap().get("traceId");
        assertThat(logTraceId).as("业务日志 MDC 里的 traceId").isNotBlank();

        // (3) 三处一致
        assertThat(logTraceId).as("日志 traceId 应与响应头一致").isEqualTo(headerTraceId);
        assertThat(bodyTraceId).as("响应体 traceId 应与响应头一致").isEqualTo(headerTraceId);
    }

    @SpringBootApplication
    static class TestApp {
        @Bean
        TraceTestController traceTestController() {
            return new TraceTestController();
        }
    }

    @RestController
    static class TraceTestController {
        private static final org.slf4j.Logger log = LoggerFactory.getLogger(TraceTestController.class);

        @GetMapping("/trace-probe")
        public Result<String> probe() {
            // 模拟一行普通业务日志 —— 验证它带不带 traceId。
            log.info("handling /trace-probe");
            return Result.ok("pong");
        }
    }
}
