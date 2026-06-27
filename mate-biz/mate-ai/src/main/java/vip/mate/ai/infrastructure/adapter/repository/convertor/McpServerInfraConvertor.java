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
package vip.mate.ai.infrastructure.adapter.repository.convertor;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import vip.mate.ai.domain.model.aggregate.McpServerAggregate;
import vip.mate.ai.infrastructure.dao.po.McpServerPO;

/**
 * MapStruct convertor between {@link McpServerAggregate} and {@link McpServerPO}.
 * State fields have no public setter, so reconstruction goes through the
 * Lombok {@code @SuperBuilder} builder.
 *
 * @author mateaix
 */
@Mapper
public interface McpServerInfraConvertor {

    McpServerInfraConvertor INSTANCE = Mappers.getMapper(McpServerInfraConvertor.class);

    @Mapping(target = "deleted", ignore = true)
    McpServerPO toPO(McpServerAggregate aggregate);

    McpServerAggregate toDomain(McpServerPO po);
}
