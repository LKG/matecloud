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
package vip.mate.crm.customer.application.command;

import lombok.Data;

/**
 * 客户报备/编辑命令。
 *
 * @author mateaix
 */
@Data
public class CustomerCommand {

    private String customerName;
    private String industry;
    private String level;
    private String source;
    private String status;
    private String phone;
    private String address;
    private String website;
    private String remark;
    /** true=直接进公海(无负责人); false=报备进私海占负责人 */
    private boolean toPublicPool;
}
