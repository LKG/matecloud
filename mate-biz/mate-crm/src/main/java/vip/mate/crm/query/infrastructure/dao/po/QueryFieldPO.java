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
package vip.mate.crm.query.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 可查字段元数据持久化对象(mate_crm_query_field)。
 *
 * <p>只读配置表, 不做软删(省略 {@code @TableLogic}), 平台不会删除种子/配置行。
 *
 * @author mateaix
 */
@Data
@TableName("mate_crm_query_field")
public class QueryFieldPO {

    @TableId
    private String id;

    private String moduleKey;
    private String fieldKey;

    /** 真实物理列名(白名单来源)。 */
    @TableField("column_name")
    private String columnName;

    private String label;
    private String fieldType;

    @TableField("options_json")
    private String optionsJson;

    private Integer sort;
    private Integer visible;
}
