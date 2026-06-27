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
package vip.mate.system.tenant.domain.adapter.repository;

import vip.mate.base.result.PageResult;
import vip.mate.system.tenant.domain.model.aggregate.TenantAggregate;
import vip.mate.system.tenant.domain.model.entity.TenantPackage;

import java.time.LocalDateTime;
import java.util.List;

public interface TenantRepository {

    void save(TenantAggregate aggregate);

    void update(TenantAggregate aggregate);

    TenantAggregate findById(String id);

    TenantAggregate findByCode(String tenantCode);

    boolean existsByCode(String tenantCode);

    PageResult<TenantAggregate> pageQuery(String keyword, int pageNum, int pageSize);

    /**
     * ACTIVE tenants whose expire_at is non-null and strictly before {@code cutoff},
     * ordered by expire_at ascending, capped at {@code limit}.
     */
    List<TenantAggregate> findActiveExpiredBefore(LocalDateTime cutoff, int limit);

    /**
     * ACTIVE tenants whose expire_at falls within [{@code from}, {@code to}],
     * used for renewal reminders.
     */
    List<TenantAggregate> findExpiringBetween(LocalDateTime from, LocalDateTime to);

    List<TenantPackage> listPackages();

    TenantPackage findPackageById(String id);

    void savePackage(TenantPackage pkg);

    void updatePackage(TenantPackage pkg);

    void deletePackage(String id);
}
