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
package vip.mate.crm.query.domain.adapter.repository;

import vip.mate.crm.query.domain.model.SavedFilter;

import java.util.List;

/**
 * 个人常用筛选仓储接口(domain 定义, infrastructure 实现)。
 *
 * @author mateaix
 */
public interface ISavedFilterRepository {

    /** 某用户在某模块下的常用筛选(按 sort 升序)。 */
    List<SavedFilter> listByUser(String userId, String moduleKey);

    SavedFilter findById(String id);

    void save(SavedFilter filter);

    void update(SavedFilter filter);

    void deleteById(String id);
}
