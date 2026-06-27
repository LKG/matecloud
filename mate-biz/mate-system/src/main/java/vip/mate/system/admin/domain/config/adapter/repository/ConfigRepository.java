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
package vip.mate.system.admin.domain.config.adapter.repository;

import vip.mate.system.admin.domain.config.model.entity.Config;
import vip.mate.base.result.PageResult;

import java.util.List;

public interface ConfigRepository {
    void save(Config config);
    void update(Config config);
    void deleteById(String id);
    Config findById(String id);
    Config findByKey(String configKey);
    boolean existsByKey(String configKey);
    /** Full list — used by RPC consumers and the in-process config cache. */
    List<Config> list();
    /**
     * Paginated list for the admin UI.
     *
     * @param keyword fuzzy match against {@code config_key} and {@code config_name}
     * @param builtIn {@code true}/{@code false} to filter by built-in flag, {@code null} for all
     */
    PageResult<Config> page(int pageNum, int pageSize, String keyword, Boolean builtIn);
}
