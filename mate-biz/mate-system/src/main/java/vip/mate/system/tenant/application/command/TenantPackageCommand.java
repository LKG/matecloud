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
package vip.mate.system.tenant.application.command;

import lombok.Data;

@Data
public class TenantPackageCommand {

    private String id;
    private String packageCode;
    private String packageName;
    private Integer maxUsers;
    private Long maxStorage;
    /** Comma-separated feature flags. */
    private String features;
    /** Whether AI is enabled for the package (0/1). */
    private Integer aiEnabled;
    /** Daily AI call cap (0 = unlimited). */
    private Integer aiQuotaDaily;
    /** Max app installs (0 = unlimited). */
    private Integer maxApps;
    /** Monthly price in cents. */
    private Integer price;
    private String remark;
}
