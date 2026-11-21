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
package vip.mate.crm.lead.types.enums;

/**
 * 线索状态机: FOLLOWING → CONVERTED(转化) / INVALID(废弃), 终态不可逆。
 *
 * @author mateaix
 */
public enum LeadStatus {

    /** 跟进中 */
    FOLLOWING,
    /** 已转化为客户 */
    CONVERTED,
    /** 已废弃 */
    INVALID
}
