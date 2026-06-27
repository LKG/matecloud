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

import vip.mate.system.admin.domain.dict.model.entity.DictData;
import vip.mate.base.result.PageResult;

import java.util.List;

public interface DictRepository {

    void save(DictData dictData);

    void update(DictData dictData);

    void deleteById(String id);

    DictData findById(String id);

    List<DictData> findByType(String dictType);

    String findLabel(String dictType, String dictValue);

    /**
     * Paginated entries scoped to one {@code dictType}. {@code keyword} matches
     * both {@code dict_label} and {@code dict_value} (substring; case-folding
     * follows the underlying column collation).
     */
    PageResult<DictData> page(String dictType, int pageNum, int pageSize, String keyword);
}
