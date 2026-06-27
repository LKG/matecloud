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
package vip.mate.system.admin.infrastructure.adapter.repository.convertor;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import vip.mate.system.admin.domain.dict.model.entity.DictData;
import vip.mate.system.admin.infrastructure.dao.po.DictDataPO;

/**
 * Convert between the {@link DictData} domain entity and {@link DictDataPO}.
 *
 * <p>{@code deleted} is a persistence-only flag on the PO (filled by MyBatis-Plus
 * {@code @TableLogic} / {@code FieldFill.INSERT}); it has no domain counterpart, so
 * it is ignored on the way out. The reverse direction simply drops it.
 *
 * @author mateaix
 */
@Mapper
public interface DictDataInfraConvertor {

    DictDataInfraConvertor INSTANCE = Mappers.getMapper(DictDataInfraConvertor.class);

    @Mapping(target = "deleted", ignore = true)
    DictDataPO toPO(DictData dictData);

    DictData toEntity(DictDataPO po);
}
