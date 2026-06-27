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

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mate_operation_log")
public class OperationLogPO {
    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    private String userId;
    private String username;
    private String module;
    private String operationType;
    private String requestMethod;
    private String requestUrl;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private String requestParams;
    private String responseResult;
    private String clientIp;
    private String userAgent;
    private String description;
    private Integer status;
    private String errorMsg;
    private Long duration;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
