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

import com.alibaba.csp.sentinel.slots.block.BlockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Import;
import vip.mate.starter.sentinel.handler.SentinelGlobalExceptionAdvice;

/**
 * Auto-configuration for mate-sentinel-starter.
 * Sentinel rules are loaded dynamically from Nacos via the Spring Cloud Alibaba
 * Sentinel datasource configuration.
 *
 * @author mateaix
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(BlockException.class)
@Import(SentinelGlobalExceptionAdvice.class)
public class SentinelAutoConfiguration {

    public SentinelAutoConfiguration() {
        log.info("[mate-sentinel] Sentinel auto-configuration activated");
    }
}
