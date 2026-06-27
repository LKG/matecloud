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
package vip.mate.starter.security.audit;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Spring ApplicationEvent payload for audit log entries.
 *
 * @author mateaix
 */
@Data
@Builder
public class AuditLogEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String module;
    private String operation;
    private String method;
    private String url;
    private String params;
    private String result;
    private String ip;
    private String userId;
    private Long costTime;
    private LocalDateTime createdAt;
    private boolean success;
    private String errorMsg;
}
