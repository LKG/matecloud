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
package vip.mate.system.domain.adapter.repository;

import vip.mate.system.domain.model.aggregate.UserAggregate;

import vip.mate.base.result.PageResult;

import java.util.List;

public interface UserRepository {

    void save(UserAggregate userAggregate);

    void update(UserAggregate userAggregate);

    /** Logical delete (sets deleted=1 via MyBatis-Plus @TableLogic). */
    void delete(String id);

    UserAggregate findById(String id);

    UserAggregate findByMobile(String mobile);

    UserAggregate findByUsername(String username);

    boolean existsByMobile(String mobile);

    boolean existsByUsername(String username);

    PageResult<UserAggregate> pageQuery(String keyword, int pageNum, int pageSize);

    /** @deprecated use pageQuery with keyword */
    default List<UserAggregate> pageQueryAll(int pageNum, int pageSize) {
        return pageQuery(null, pageNum, pageSize).getList();
    }
}
