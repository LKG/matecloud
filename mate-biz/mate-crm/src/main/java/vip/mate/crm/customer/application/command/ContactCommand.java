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
 * 联系人新增/编辑命令。
 *
 * @author mateaix
 */
@Data
public class ContactCommand {

    private String customerId;
    private String customerName;
    private String contactName;
    private String mobile;
    private String telephone;
    private String email;
    private String position;
    private String gender;
    /** 1=设为主联系人 */
    private Integer isPrimary;
    private String wechat;
    private String remark;
}
