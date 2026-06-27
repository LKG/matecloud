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
package vip.mate.system.application.convertor;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import vip.mate.api.system.enums.UserStatus;
import vip.mate.api.system.response.UserInfoResponse;
import vip.mate.system.domain.model.aggregate.UserAggregate;
import vip.mate.system.domain.model.entity.User;

/**
 * Convert domain aggregate/entity to API response DTO.
 * Maps the internal domain UserStatus to the public mate-api UserStatus enum.
 *
 * @author mateaix
 */
@Mapper(componentModel = "spring")
public interface UserConvertor {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.tenantId", target = "tenantId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.realName", target = "realName")
    @Mapping(source = "user.mobile", target = "mobile")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "user.avatar", target = "avatar")
    @Mapping(source = "user.status", target = "status", qualifiedByName = "mapStatus")
    @Mapping(source = "user.createdAt", target = "createdAt")
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "roleCodes", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    @Mapping(target = "deptId", ignore = true)
    @Mapping(target = "deptName", ignore = true)
    UserInfoResponse toResponse(UserAggregate aggregate);

    @Named("mapStatus")
    default UserStatus mapStatus(vip.mate.system.domain.model.valobj.UserStatus status) {
        if (status == null) return null;
        return switch (status) {
            case ACTIVE -> UserStatus.ACTIVE;
            case FROZEN -> UserStatus.DISABLED;
            case DELETED -> UserStatus.DELETED;
        };
    }

    /**
     * Include hashed password in the response (for auth RPC only).
     */
    default UserInfoResponse toResponseWithPassword(UserAggregate aggregate) {
        UserInfoResponse response = toResponse(aggregate);
        if (response != null && aggregate != null) {
            User u = aggregate.getUser();
            if (u != null) {
                response.setPassword(u.getPassword());
            }
        }
        return response;
    }
}
