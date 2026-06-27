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
package vip.mate.system.tenant.application.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.mate.base.exception.BizException;
import vip.mate.base.response.ResponseCode;
import vip.mate.system.tenant.application.provision.TenantProvisionService;
import vip.mate.system.tenant.domain.adapter.repository.TenantRepository;
import vip.mate.system.tenant.domain.model.aggregate.TenantAggregate;
import vip.mate.system.tenant.domain.model.entity.TenantPackage;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantCommandService {

    private final TenantRepository tenantRepository;
    private final TenantProvisionService tenantProvisionService;

    @Transactional(rollbackFor = Exception.class)
    public String create(TenantCommand cmd) {
        if (tenantRepository.existsByCode(cmd.getTenantCode())) {
            throw new BizException(ResponseCode.DUPLICATE_DATA.getCode(),
                    "tenantCode already exists: " + cmd.getTenantCode());
        }
        TenantAggregate aggregate = TenantAggregate.create(
                cmd.getTenantCode(), cmd.getTenantName(), cmd.getPackageId(),
                cmd.getContactName(), cmd.getContactPhone());
        aggregate.updateProfile(cmd.getTenantName(), cmd.getContactName(),
                cmd.getContactPhone(), cmd.getContactEmail(), cmd.getDomain(), cmd.getRemark());
        if (cmd.getExpireAt() != null) {
            aggregate.renew(cmd.getExpireAt());
        }
        tenantRepository.save(aggregate);
        // Provision role + admin + default menus so the tenant is immediately
        // usable. Runs in this same transaction — any failure rolls the tenant back.
        tenantProvisionService.provision(aggregate.getId(), cmd.getTenantCode(), cmd.getContactName());
        return aggregate.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(TenantCommand cmd) {
        TenantAggregate aggregate = mustFind(cmd.getId());
        aggregate.updateProfile(cmd.getTenantName(), cmd.getContactName(),
                cmd.getContactPhone(), cmd.getContactEmail(), cmd.getDomain(), cmd.getRemark());
        if (cmd.getPackageId() != null && !cmd.getPackageId().isBlank()) {
            aggregate.changePackage(cmd.getPackageId());
        }
        tenantRepository.update(aggregate);
    }

    @Transactional(rollbackFor = Exception.class)
    public void suspend(String id) {
        TenantAggregate aggregate = mustFind(id);
        aggregate.suspend();
        tenantRepository.update(aggregate);
    }

    @Transactional(rollbackFor = Exception.class)
    public void activate(String id) {
        TenantAggregate aggregate = mustFind(id);
        aggregate.activate();
        tenantRepository.update(aggregate);
    }

    @Transactional(rollbackFor = Exception.class)
    public void renew(String id, LocalDateTime newExpireAt) {
        TenantAggregate aggregate = mustFind(id);
        aggregate.renew(newExpireAt);
        tenantRepository.update(aggregate);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        TenantAggregate aggregate = mustFind(id);
        aggregate.delete();
        tenantRepository.update(aggregate);
    }

    private TenantAggregate mustFind(String id) {
        TenantAggregate aggregate = tenantRepository.findById(id);
        if (aggregate == null) {
            throw new BizException(ResponseCode.DATA_NOT_FOUND.getCode(),
                    "Tenant not found: " + id);
        }
        return aggregate;
    }

    // ==================== Tenant Package CRUD ====================

    @Transactional(rollbackFor = Exception.class)
    public String createPackage(TenantPackageCommand cmd) {
        TenantPackage pkg = TenantPackage.builder()
                .id(UUID.randomUUID().toString().replace("-", ""))
                .packageCode(cmd.getPackageCode())
                .packageName(cmd.getPackageName())
                .maxUsers(cmd.getMaxUsers())
                .maxStorage(cmd.getMaxStorage())
                .features(cmd.getFeatures())
                .aiEnabled(cmd.getAiEnabled())
                .aiQuotaDaily(cmd.getAiQuotaDaily())
                .maxApps(cmd.getMaxApps())
                .price(cmd.getPrice())
                .remark(cmd.getRemark())
                .build();
        tenantRepository.savePackage(pkg);
        return pkg.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updatePackage(TenantPackageCommand cmd) {
        TenantPackage pkg = tenantRepository.findPackageById(cmd.getId());
        if (pkg == null) {
            throw new BizException(ResponseCode.DATA_NOT_FOUND.getCode(),
                    "Tenant package not found: " + cmd.getId());
        }
        // packageCode is immutable once created.
        pkg.setPackageName(cmd.getPackageName());
        pkg.setMaxUsers(cmd.getMaxUsers());
        pkg.setMaxStorage(cmd.getMaxStorage());
        pkg.setFeatures(cmd.getFeatures());
        pkg.setAiEnabled(cmd.getAiEnabled());
        pkg.setAiQuotaDaily(cmd.getAiQuotaDaily());
        pkg.setMaxApps(cmd.getMaxApps());
        pkg.setPrice(cmd.getPrice());
        pkg.setRemark(cmd.getRemark());
        tenantRepository.updatePackage(pkg);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deletePackage(String id) {
        tenantRepository.deletePackage(id);
    }
}
