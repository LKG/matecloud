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
package vip.mate.crm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * MateCloud CRM service — 对标悟空 CRM 的销售全流程:
 * 线索 → 客户(公海/私海) → 联系人/跟进 → 商机(阶段漏斗) → 合同(审批)
 * → 回款计划/回款 → 发票 → 售后工单, 外加工作台待办、数据看板与 AI 助手。
 *
 * <p>Dual-role: runs standalone (port 9070) or merged into mate-monolith.
 *
 * @author mateaix
 */
@SpringBootApplication(scanBasePackages = "vip.mate.crm")
@EnableDiscoveryClient
public class MateCrmApplication {

    public static void main(String[] args) {
        SpringApplication.run(MateCrmApplication.class, args);
    }
}
