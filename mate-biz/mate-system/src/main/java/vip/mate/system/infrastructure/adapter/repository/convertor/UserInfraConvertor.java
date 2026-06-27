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
package vip.mate.system.infrastructure.adapter.repository.convertor;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import vip.mate.system.domain.constant.UserOperateType;
import vip.mate.system.domain.model.entity.User;
import vip.mate.system.domain.model.entity.UserOperateStream;
import vip.mate.system.domain.model.valobj.UserStatus;
import vip.mate.system.infrastructure.dao.po.UserOperateStreamPO;
import vip.mate.system.infrastructure.dao.po.UserPO;

/**
 * Convert between domain entities and PO.
 *
 * @author mateaix
 */
@Mapper
public interface UserInfraConvertor {

    UserInfraConvertor INSTANCE = Mappers.getMapper(UserInfraConvertor.class);

    @Mapping(source = "status", target = "status", qualifiedByName = "statusToCode")
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "lockVersion", ignore = true)
    UserPO toPO(User user);

    @Mapping(source = "status", target = "status", qualifiedByName = "codeToStatus")
    User toEntity(UserPO po);

    @Mapping(source = "operateType", target = "operateType", qualifiedByName = "operateTypeToCode")
    @Mapping(target = "deleted", ignore = true)
    UserOperateStreamPO toStreamPO(UserOperateStream stream);

    @Named("statusToCode")
    default Integer statusToCode(UserStatus status) {
        return status == null ? null : status.getCode();
    }

    @Named("codeToStatus")
    default UserStatus codeToStatus(Integer code) {
        return UserStatus.fromCode(code);
    }

    @Named("operateTypeToCode")
    default String operateTypeToCode(UserOperateType type) {
        return type == null ? null : type.getCode();
    }
}
