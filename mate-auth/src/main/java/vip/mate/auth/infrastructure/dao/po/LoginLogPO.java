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
package vip.mate.auth.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Persistent shape for {@code mate_login_log}. Mirrors the row layout in
 * mate-admin's V1 schema; kept private to mate-auth so the auth-side
 * write path doesn't pull in mate-admin as a peer dependency.
 *
 * <p>{@link vip.mate.admin.infrastructure.dao.po.LoginLogPO} is the read
 * side — both ends agree on the column names and there's a single Flyway
 * migration owning the table (V1 in mate-admin).
 *
 * @author mateaix
 */
@Data
@TableName("mate_login_log")
public class LoginLogPO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String username;

    @TableField("client_ip")
    private String clientIp;

    @TableField("user_agent")
    private String userAgent;

    @TableField("login_type")
    private String loginType;

    /** 0 = success, 1 = failure. Matches the values on the read side. */
    private Integer status;

    @TableField("fail_msg")
    private String failMsg;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
