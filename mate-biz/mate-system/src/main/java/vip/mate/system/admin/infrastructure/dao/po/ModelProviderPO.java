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
package vip.mate.system.admin.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI model provider config row. {@code config_json} holds the provider's credential
 * field values as a JSON object, AES-encrypted as a whole; {@code modalities} is a
 * comma-separated list of supported model types (e.g. {@code LLM,EMBEDDING}).
 *
 * @author mateaix
 */
@Data
@TableName("mate_model_provider")
public class ModelProviderPO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("scope")
    private String scope;

    @TableField("vendor")
    private String vendor;

    @TableField("name")
    private String name;

    @TableField("modalities")
    private String modalities;

    @TableField("config_json")
    private String configJson;

    /** JSON array of available model ids (plaintext), e.g. {@code ["gpt-4o","gpt-4o-mini"]}. */
    @TableField("models")
    private String models;

    @TableField("enabled")
    private Integer enabled;

    @TableField("sort")
    private Integer sort;

    @TableLogic
    @TableField(value = "deleted", fill = FieldFill.INSERT)
    private Integer deleted;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
