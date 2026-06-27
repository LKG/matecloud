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
package vip.mate.system.domain.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import vip.mate.base.model.entity.BaseEntity;
import vip.mate.system.domain.constant.UserOperateType;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserOperateStream extends BaseEntity {

    private String userId;
    private UserOperateType operateType;
    private String operateDetail;
    private LocalDateTime operateTime;

    public static UserOperateStream create(String userId, UserOperateType type, String detail) {
        return UserOperateStream.builder()
                .id(UUID.randomUUID().toString().replace("-", ""))
                .userId(userId)
                .operateType(type)
                .operateDetail(detail)
                .operateTime(LocalDateTime.now())
                .build();
    }
}
