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

import java.time.LocalDateTime;

/**
 * MCP server aggregate. The {@code status} flag and health-check results
 * ({@code lastCheckAt} / {@code lastError}) carry no public setter — they change
 * only via the methods below (DDD rule 3). Reconstruction from persistence goes
 * through the builder.
 *
 * @author mateaix
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class McpServerAggregate extends BaseEntity {

    private String code;
    private String name;
    private String transport;
    private String command;
    private String args;
    private String endpoint;
    private String envJson;
    private String description;
    private Integer toolCount;
    @Setter(AccessLevel.NONE)
    private Integer status;
    @Setter(AccessLevel.NONE)
    private LocalDateTime lastCheckAt;
    @Setter(AccessLevel.NONE)
    private String lastError;
    private Integer sort;

    public static McpServerAggregate newOne(String code, String name, String transport) {
        return McpServerAggregate.builder()
                .id(SnowflakeUtil.newSnowflakeId())
                .code(code)
                .name(name)
                .transport(transport == null ? "STDIO" : transport)
                .status(0)
                .toolCount(0)
                .sort(0)
                .build();
    }

    public boolean isEnabled() {
        return status != null && status == 1;
    }

    public void enable() {
        this.status = 1;
    }

    public void disable() {
        this.status = 0;
    }

    /** Record the outcome of a connectivity/health check. */
    public void recordCheck(boolean ok, LocalDateTime at, String error) {
        this.status = ok ? 1 : 0;
        this.lastCheckAt = at;
        this.lastError = ok ? null : error;
    }

    /** Merge editable fields from an input carrier; {@code null} fields keep the current value. */
    public void applyUpdate(McpServerAggregate src) {
        if (src.getName() != null) this.name = src.getName();
        if (src.getTransport() != null) this.transport = src.getTransport();
        if (src.getCommand() != null) this.command = src.getCommand();
        if (src.getArgs() != null) this.args = src.getArgs();
        if (src.getEndpoint() != null) this.endpoint = src.getEndpoint();
        if (src.getEnvJson() != null) this.envJson = src.getEnvJson();
        if (src.getDescription() != null) this.description = src.getDescription();
        if (src.getToolCount() != null) this.toolCount = src.getToolCount();
        if (src.getStatus() != null) this.status = src.getStatus();
        if (src.getSort() != null) this.sort = src.getSort();
    }
}
