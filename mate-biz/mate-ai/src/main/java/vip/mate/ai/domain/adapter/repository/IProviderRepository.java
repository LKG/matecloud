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
package vip.mate.ai.domain.adapter.repository;

import vip.mate.ai.domain.model.aggregate.ProviderAggregate;
import vip.mate.base.result.PageResult;

import java.util.List;

public interface IProviderRepository {

    void save(ProviderAggregate provider);

    void update(ProviderAggregate provider);

    void deleteById(String id);

    ProviderAggregate findById(String id);

    ProviderAggregate findByCode(String code);

    /** Clear the default flag on all rows except the given id. */
    void clearDefaultExcept(String keepId);

    PageResult<ProviderAggregate> page(int pageNum, int pageSize, String keyword, String vendor, Integer enabled);

    List<ProviderAggregate> listEnabled();
}
