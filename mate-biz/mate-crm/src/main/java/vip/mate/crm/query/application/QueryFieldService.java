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
package vip.mate.crm.query.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vip.mate.crm.query.domain.adapter.repository.IQueryFieldRepository;
import vip.mate.crm.query.domain.model.QueryField;
import vip.mate.crm.query.dto.QueryFieldMeta;
import vip.mate.crm.query.types.enums.FilterOperator;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 可查字段元数据服务: 缓存(低频变更) + 下发前端筛选器配置。
 *
 * @author mateaix
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryFieldService {

    private final IQueryFieldRepository queryFieldRepository;
    private final ObjectMapper objectMapper;

    /** moduleKey -> 元数据快照; 管理员改配置后可 {@link #evict(String)} 失效。 */
    private final Map<String, List<QueryFieldMeta>> cache = new ConcurrentHashMap<>();

    public List<QueryFieldMeta> list(String moduleKey) {
        return cache.computeIfAbsent(moduleKey, this::load);
    }

    /** 使某模块缓存失效(供后续元数据管理功能调用)。 */
    public void evict(String moduleKey) {
        cache.remove(moduleKey);
    }

    private List<QueryFieldMeta> load(String moduleKey) {
        return queryFieldRepository.listByModule(moduleKey).stream()
                .filter(QueryField::visible)
                .map(this::toMeta)
                .toList();
    }

    private QueryFieldMeta toMeta(QueryField field) {
        QueryFieldMeta meta = new QueryFieldMeta();
        meta.setFieldKey(field.fieldKey());
        meta.setLabel(field.label());
        meta.setFieldType(field.fieldType().name());
        meta.setOperators(field.fieldType().allowedOperators().stream()
                .map(FilterOperator::name).sorted().toList());
        meta.setOptions(parseOptions(field.optionsJson()));
        return meta;
    }

    private List<QueryFieldMeta.OptionItem> parseOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(optionsJson, new TypeReference<List<QueryFieldMeta.OptionItem>>() {
            });
        } catch (Exception e) {
            log.warn("Failed to parse options_json for query field: {}", optionsJson, e);
            return List.of();
        }
    }
}
