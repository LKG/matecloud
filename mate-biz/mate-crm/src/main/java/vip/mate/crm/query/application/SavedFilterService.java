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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vip.mate.base.exception.BizException;
import vip.mate.crm.common.CurrentUserResolver;
import vip.mate.crm.query.domain.adapter.repository.ISavedFilterRepository;
import vip.mate.crm.query.domain.model.SavedFilter;
import vip.mate.crm.query.dto.AdvancedQuery;
import vip.mate.crm.query.dto.SavedFilterRequest;
import vip.mate.crm.query.dto.SavedFilterVO;
import vip.mate.crm.types.exception.CrmErrorCode;
import vip.mate.starter.distribute.util.SnowflakeUtil;

import java.util.List;

/**
 * 个人常用筛选服务: 按当前用户隔离的 CRUD(仅本人可见)。
 *
 * @author mateaix
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SavedFilterService {

    private final ISavedFilterRepository savedFilterRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public List<SavedFilterVO> list(String moduleKey) {
        String userId = currentUserResolver.require().userId();
        return savedFilterRepository.listByUser(userId, moduleKey).stream()
                .map(this::toVO)
                .toList();
    }

    /** 新增或覆盖更新(id 非空即更新), 强制归属当前用户。 */
    public String save(SavedFilterRequest req) {
        String userId = currentUserResolver.require().userId();
        String json = writeJson(req.getQuery());
        int sort = req.getSort() == null ? 0 : req.getSort();

        if (req.getId() != null && !req.getId().isBlank()) {
            SavedFilter existing = ownedOrThrow(req.getId(), userId);
            existing.setName(req.getName());
            existing.setModuleKey(req.getModuleKey());
            existing.setQueryJson(json);
            existing.setSort(sort);
            savedFilterRepository.update(existing);
            return existing.getId();
        }
        SavedFilter filter = SavedFilter.builder()
                .id(SnowflakeUtil.newSnowflakeId())
                .userId(userId)
                .moduleKey(req.getModuleKey())
                .name(req.getName())
                .queryJson(json)
                .sort(sort)
                .build();
        savedFilterRepository.save(filter);
        return filter.getId();
    }

    public void delete(String id) {
        String userId = currentUserResolver.require().userId();
        ownedOrThrow(id, userId);
        savedFilterRepository.deleteById(id);
    }

    private SavedFilter ownedOrThrow(String id, String userId) {
        SavedFilter filter = savedFilterRepository.findById(id);
        if (filter == null || !userId.equals(filter.getUserId())) {
            throw new BizException(CrmErrorCode.NO_PERMISSION.getCode(), "常用筛选不存在或无权操作");
        }
        return filter;
    }

    private SavedFilterVO toVO(SavedFilter filter) {
        return new SavedFilterVO(filter.getId(), filter.getModuleKey(), filter.getName(),
                readQuery(filter.getQueryJson()), filter.getSort());
    }

    private String writeJson(AdvancedQuery query) {
        try {
            return objectMapper.writeValueAsString(query == null ? new AdvancedQuery() : query);
        } catch (Exception e) {
            throw new BizException(CrmErrorCode.OPERATOR_TYPE_MISMATCH.getCode(), "筛选条件序列化失败");
        }
    }

    private AdvancedQuery readQuery(String json) {
        if (json == null || json.isBlank()) {
            return new AdvancedQuery();
        }
        try {
            return objectMapper.readValue(json, AdvancedQuery.class);
        } catch (Exception e) {
            log.warn("Failed to deserialize saved filter query_json: {}", json, e);
            return new AdvancedQuery();
        }
    }
}
