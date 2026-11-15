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
package vip.mate.starter.gray;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 进程内特性灰度配置 (monolith 路线 B)。
 *
 * <p>默认关闭：未显式 {@code mate.gray.enabled=true} 时 {@link GrayServletFilter}
 * 不注册、不改变任何现有请求行为。</p>
 *
 * @author mateaix
 */
@ConfigurationProperties(prefix = "mate.gray")
public class GrayProperties {

    /** 是否启用进程内灰度过滤器。默认 false —— 灰度须显式开启。 */
    private boolean enabled = false;

    /** Redis 规则 key 前缀，实际 key 为 {@code {keyPrefix}{feature}}。 */
    private String keyPrefix = "mate:gray:rule:";

    /** 过滤器为请求默认解析的特性 key（整包灰度时用）。 */
    private String defaultFeature = "default";

    /** 规则快照本地缓存 TTL（毫秒），到期后惰性回源 Redis。 */
    private long cacheTtlMillis = 1000L;

    /**
     * 是否信任入站 {@code X-Gray-Version} 头（信任边缘/反代已置入的版本头做覆盖）。
     * 默认 false —— 只按 Redis 规则 + userId 决策，避免浏览器伪造头进灰度。
     */
    private boolean trustHeader = false;

    /** 读取身份的 userId 请求头名（单体里由 MonolithHeaderRelayFilter 注入）。 */
    private String userIdHeader = "X-User-Id";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public String getDefaultFeature() {
        return defaultFeature;
    }

    public void setDefaultFeature(String defaultFeature) {
        this.defaultFeature = defaultFeature;
    }

    public long getCacheTtlMillis() {
        return cacheTtlMillis;
    }

    public void setCacheTtlMillis(long cacheTtlMillis) {
        this.cacheTtlMillis = cacheTtlMillis;
    }

    public boolean isTrustHeader() {
        return trustHeader;
    }

    public void setTrustHeader(boolean trustHeader) {
        this.trustHeader = trustHeader;
    }

    public String getUserIdHeader() {
        return userIdHeader;
    }

    public void setUserIdHeader(String userIdHeader) {
        this.userIdHeader = userIdHeader;
    }
}
