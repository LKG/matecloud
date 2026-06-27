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
package vip.mate.system.admin.domain.dict.adapter.repository;

import vip.mate.system.admin.domain.dict.model.entity.DictType;
import vip.mate.base.result.PageResult;

public interface DictTypeRepository {

    void save(DictType dictType);

    void update(DictType dictType);

    void deleteById(String id);

    DictType findById(String id);

    DictType findByCode(String code);

    boolean existsByCode(String code);

    /**
     * Paginated list of dict types. {@code keyword} matches both the code
     * and the human name (case-insensitive). {@code null} keyword returns
     * all types ordered by created_at desc.
     */
    PageResult<DictType> page(int pageNum, int pageSize, String keyword);
}
