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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Provider aggregate. State-bearing fields (enabled / isDefault / last-test
 * result / encrypted api key) carry <b>no public setter</b> — they change only
 * through the intent-revealing methods below, so a caller can't shove the
 * aggregate into an arbitrary state (DDD rule 3). Reconstruction from
 * persistence goes through the builder, which bypasses these locks.
 *
 * @author mateaix
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ProviderAggregate extends BaseEntity {

    private String code;
    private String name;
    private String vendor;
    private String baseUrl;
    /** Stored ALREADY encrypted; never plain text. */
    @Setter(AccessLevel.NONE)
    private String apiKeyCipher;
    private String defaultModel;
    private String availableModels;
    private BigDecimal temperature;
    private Integer maxTokens;
    @Setter(AccessLevel.NONE)
    private Integer enabled;
    @Setter(AccessLevel.NONE)
    private Integer isDefault;
    @Setter(AccessLevel.NONE)
    private LocalDateTime lastTestAt;
    @Setter(AccessLevel.NONE)
    private Integer lastTestOk;
    private Integer sort;

    public static ProviderAggregate newOne(String code, String name, String vendor) {
        return ProviderAggregate.builder()
                .id(SnowflakeUtil.newSnowflakeId())
                .code(code)
                .name(name)
                .vendor(vendor == null ? "CUSTOM" : vendor)
                .enabled(1)
                .isDefault(0)
                .sort(0)
                .build();
    }

    public boolean isEnabled() {
        return enabled != null && enabled == 1;
    }

    public boolean isDefaultProvider() {
        return isDefault != null && isDefault == 1;
    }

    public void enable() {
        this.enabled = 1;
    }

    public void disable() {
        this.enabled = 0;
    }

    /** Flag this provider as the default; cross-row uniqueness is enforced by the repository. */
    public void markAsDefault() {
        this.isDefault = 1;
    }

    public void clearDefault() {
        this.isDefault = 0;
    }

    /** Store an already-encrypted API key (encryption itself is an infrastructure concern). */
    public void assignApiKey(String cipher) {
        this.apiKeyCipher = cipher;
    }

    /** Record the outcome of a connectivity/credentials test. */
    public void markTested(boolean ok, LocalDateTime at) {
        this.lastTestAt = at;
        this.lastTestOk = ok ? 1 : 0;
    }

    /** Merge editable fields from an input carrier; {@code null} fields keep the current value. */
    public void applyUpdate(ProviderAggregate src) {
        if (src.getName() != null) this.name = src.getName();
        if (src.getVendor() != null) this.vendor = src.getVendor();
        if (src.getBaseUrl() != null) this.baseUrl = src.getBaseUrl();
        if (src.getDefaultModel() != null) this.defaultModel = src.getDefaultModel();
        if (src.getAvailableModels() != null) this.availableModels = src.getAvailableModels();
        if (src.getTemperature() != null) this.temperature = src.getTemperature();
        if (src.getMaxTokens() != null) this.maxTokens = src.getMaxTokens();
        if (src.getEnabled() != null) this.enabled = src.getEnabled();
        if (src.getIsDefault() != null) this.isDefault = src.getIsDefault();
        if (src.getSort() != null) this.sort = src.getSort();
    }
}
