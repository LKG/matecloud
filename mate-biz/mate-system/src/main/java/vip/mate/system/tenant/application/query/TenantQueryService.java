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
package vip.mate.system.tenant.application.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vip.mate.base.result.PageResult;
import vip.mate.system.tenant.domain.adapter.repository.TenantRepository;
import vip.mate.system.tenant.domain.model.aggregate.TenantAggregate;
import vip.mate.system.tenant.domain.model.entity.Tenant;
import vip.mate.system.tenant.domain.model.entity.TenantPackage;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TenantQueryService {

    private final TenantRepository tenantRepository;

    public Tenant findById(String id) {
        TenantAggregate aggregate = tenantRepository.findById(id);
        return aggregate == null ? null : aggregate.getTenant();
    }

    public Tenant findByCode(String tenantCode) {
        TenantAggregate aggregate = tenantRepository.findByCode(tenantCode);
        return aggregate == null ? null : aggregate.getTenant();
    }

    public PageResult<Tenant> pageQuery(String keyword, int pageNum, int pageSize) {
        PageResult<TenantAggregate> page = tenantRepository.pageQuery(keyword, pageNum, pageSize);
        List<Tenant> list = page.getList().stream()
                .map(TenantAggregate::getTenant)
                .toList();
        return PageResult.of(list, page.getTotal());
    }

    public List<TenantPackage> listPackages() {
        return tenantRepository.listPackages();
    }
}
