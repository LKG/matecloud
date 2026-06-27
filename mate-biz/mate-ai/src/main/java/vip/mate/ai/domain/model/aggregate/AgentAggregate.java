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
package vip.mate.ai.domain.model.aggregate;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import vip.mate.base.model.entity.BaseEntity;
import vip.mate.starter.distribute.util.SnowflakeUtil;

/**
 * Agent aggregate. The {@code enabled} flag and the {@code builtIn} marker carry
 * no public setter — they change only via the methods below (DDD rule 3).
 * Reconstruction from persistence goes through the builder.
 *
 * @author mateaix
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AgentAggregate extends BaseEntity {

    private String code;
    private String name;
    private String nameEn;
    private String category;
    private String provider;
    private String description;
    private String icon;
    private String installCmd;
    private String launchCmd;
    private String defaultModel;
    private String systemPrompt;
    @Setter(AccessLevel.NONE)
    private Integer enabled;
    @Setter(AccessLevel.NONE)
    private Integer builtIn;
    private Integer sort;

    public static AgentAggregate newOne(String code, String name) {
        return AgentAggregate.builder()
                .id(SnowflakeUtil.newSnowflakeId())
                .code(code)
                .name(name)
                .category("CODING")
                .enabled(1)
                .builtIn(0)
                .sort(0)
                .build();
    }

    public boolean isBuiltIn() {
        return builtIn != null && builtIn == 1;
    }

    public boolean isEnabled() {
        return enabled != null && enabled == 1;
    }

    public void enable() {
        this.enabled = 1;
    }

    public void disable() {
        this.enabled = 0;
    }

    /** Merge editable fields from an input carrier; {@code null} fields keep the current value. */
    public void applyUpdate(AgentAggregate src) {
        if (src.getName() != null) this.name = src.getName();
        if (src.getNameEn() != null) this.nameEn = src.getNameEn();
        if (src.getCategory() != null) this.category = src.getCategory();
        if (src.getProvider() != null) this.provider = src.getProvider();
        if (src.getDescription() != null) this.description = src.getDescription();
        if (src.getIcon() != null) this.icon = src.getIcon();
        if (src.getInstallCmd() != null) this.installCmd = src.getInstallCmd();
        if (src.getLaunchCmd() != null) this.launchCmd = src.getLaunchCmd();
        if (src.getDefaultModel() != null) this.defaultModel = src.getDefaultModel();
        if (src.getSystemPrompt() != null) this.systemPrompt = src.getSystemPrompt();
        if (src.getEnabled() != null) this.enabled = src.getEnabled();
        if (src.getSort() != null) this.sort = src.getSort();
    }
}
