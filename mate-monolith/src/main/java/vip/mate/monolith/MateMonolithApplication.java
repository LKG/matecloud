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
package vip.mate.monolith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import vip.mate.auth.MateAuthApplication;
import vip.mate.notice.MateNoticeApplication;
import vip.mate.system.MateSystemApplication;

/**
 * Monolith mode entry point — runs auth + system + notice in a single JVM.
 * Activate with: MATE_RPC_MODE=local java -jar mate-monolith-exec.jar
 * <p>
 * The explicit {@link ComponentScan} pulls every module's beans (base package
 * {@code vip.mate}) into one context. The three per-service
 * {@code @SpringBootApplication} classes are themselves {@code @Configuration} +
 * {@code @ComponentScan} + {@code @EnableDiscoveryClient} (mate-system adds
 * {@code @EnableAsync}); left in the scan they would re-trigger service discovery
 * and duplicate component scanning, so they are excluded — this class is the
 * single composition root. The first two filters re-declare the exclusions that
 * {@code @SpringBootApplication}'s built-in {@code @ComponentScan} applies by
 * default (kept here because an explicit {@code @ComponentScan} replaces them).
 *
 * @author mateaix
 */
@SpringBootApplication
@ComponentScan(basePackages = "vip.mate", excludeFilters = {
        @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
        @ComponentScan.Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                MateAuthApplication.class,
                MateSystemApplication.class,
                MateNoticeApplication.class
        })
})
public class MateMonolithApplication {
    public static void main(String[] args) {
        SpringApplication.run(MateMonolithApplication.class, args);
    }
}
