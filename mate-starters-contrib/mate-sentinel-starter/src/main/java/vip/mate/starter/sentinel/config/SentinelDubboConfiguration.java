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
package vip.mate.starter.sentinel.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

/**
 * Marker configuration for Sentinel Dubbo3 adapter.
 * The actual filters are auto-registered via Dubbo SPI when the adapter jar is present.
 *
 * @author mateaix
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(name = "com.alibaba.csp.sentinel.adapter.dubbo3.SentinelDubboProviderFilter")
public class SentinelDubboConfiguration {

    public SentinelDubboConfiguration() {
        log.info("[mate-sentinel] Sentinel Dubbo3 adapter detected, filters active");
    }
}
