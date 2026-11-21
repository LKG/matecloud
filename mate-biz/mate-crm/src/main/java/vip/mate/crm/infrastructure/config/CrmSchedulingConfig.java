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
package vip.mate.crm.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * CRM 定时任务开关(公海回收 / 回款提醒)。
 *
 * <p>独立 @Configuration 而非启动类注解: 单体模式下 mate-crm 的
 * {@code @SpringBootApplication} 被 excludeFilters 排除, 但其普通配置类
 * 仍会被 base 包扫描装配, 与 mate-system 的 TenantSchedulingConfig 同构。
 *
 * @author mateaix
 */
@Configuration
@EnableScheduling
public class CrmSchedulingConfig {
}
