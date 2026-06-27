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
package vip.mate.system.tenant.domain.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import vip.mate.base.model.entity.BaseEntity;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TenantPackage extends BaseEntity {

    private String packageCode;
    private String packageName;
    private Integer maxUsers;
    private Long maxStorage;
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

    public boolean hasFeature(String feature) {
        if (features == null || features.isBlank()) return false;
        for (String f : features.split(",")) {
            if (f.trim().equalsIgnoreCase(feature)) return true;
        }
        return false;
    }

    /** Whether AI calls are permitted at all for this package. */
    public boolean isAiEnabled() {
        return aiEnabled != null && aiEnabled == 1;
    }

    /** Whether the daily AI quota is effectively unlimited (null or 0). */
    public boolean isAiQuotaUnlimited() {
        return aiQuotaDaily == null || aiQuotaDaily <= 0;
    }

    /** Whether the app-install cap is effectively unlimited (null or 0). */
    public boolean isMaxAppsUnlimited() {
        return maxApps == null || maxApps <= 0;
    }
}
