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
package vip.mate.system.tenant.infrastructure.adapter.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vip.mate.base.result.PageResult;
import vip.mate.system.tenant.domain.adapter.repository.TenantRepository;
import vip.mate.system.tenant.domain.model.aggregate.TenantAggregate;
import vip.mate.system.tenant.domain.model.entity.Tenant;
import vip.mate.system.tenant.domain.model.entity.TenantPackage;
import vip.mate.system.tenant.domain.model.valobj.TenantStatus;
import vip.mate.system.tenant.infrastructure.dao.TenantDao;
import vip.mate.system.tenant.infrastructure.dao.TenantPackageDao;
import vip.mate.system.tenant.infrastructure.dao.po.TenantPO;
import vip.mate.system.tenant.infrastructure.dao.po.TenantPackagePO;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TenantRepositoryImpl implements TenantRepository {

    private final TenantDao tenantDao;
    private final TenantPackageDao tenantPackageDao;

    @Override
    public void save(TenantAggregate aggregate) {
        tenantDao.insert(toPO(aggregate.getTenant()));
    }

    @Override
    public void update(TenantAggregate aggregate) {
        tenantDao.updateById(toPO(aggregate.getTenant()));
    }

    @Override
    public TenantAggregate findById(String id) {
        TenantPO po = tenantDao.selectById(id);
        return po == null ? null : TenantAggregate.of(toEntity(po));
    }

    @Override
    public TenantAggregate findByCode(String tenantCode) {
        LambdaQueryWrapper<TenantPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TenantPO::getTenantCode, tenantCode).last("limit 1");
        TenantPO po = tenantDao.selectOne(wrapper);
        return po == null ? null : TenantAggregate.of(toEntity(po));
    }

    @Override
    public boolean existsByCode(String tenantCode) {
        LambdaQueryWrapper<TenantPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TenantPO::getTenantCode, tenantCode);
        return tenantDao.selectCount(wrapper) > 0;
    }

    @Override
    public PageResult<TenantAggregate> pageQuery(String keyword, int pageNum, int pageSize) {
        Page<TenantPO> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TenantPO> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w
                    .like(TenantPO::getTenantCode, keyword)
                    .or().like(TenantPO::getTenantName, keyword)
                    .or().like(TenantPO::getContactName, keyword));
        }
        wrapper.orderByDesc(TenantPO::getCreatedAt);
        Page<TenantPO> result = tenantDao.selectPage(page, wrapper);
        List<TenantAggregate> list = result.getRecords().stream()
                .map(po -> TenantAggregate.of(toEntity(po)))
                .toList();
        return PageResult.of(list, result.getTotal());
    }

    @Override
    public List<TenantAggregate> findActiveExpiredBefore(LocalDateTime cutoff, int limit) {
        LambdaQueryWrapper<TenantPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TenantPO::getStatus, TenantStatus.ACTIVE.getCode())
                .isNotNull(TenantPO::getExpireAt)
                .lt(TenantPO::getExpireAt, cutoff)
                .orderByAsc(TenantPO::getExpireAt)
                .last("limit " + limit);
        return tenantDao.selectList(wrapper).stream()
                .map(po -> TenantAggregate.of(toEntity(po)))
                .toList();
    }

    @Override
    public List<TenantAggregate> findExpiringBetween(LocalDateTime from, LocalDateTime to) {
        LambdaQueryWrapper<TenantPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TenantPO::getStatus, TenantStatus.ACTIVE.getCode())
                .isNotNull(TenantPO::getExpireAt)
                .ge(TenantPO::getExpireAt, from)
                .le(TenantPO::getExpireAt, to)
                .orderByAsc(TenantPO::getExpireAt);
        return tenantDao.selectList(wrapper).stream()
                .map(po -> TenantAggregate.of(toEntity(po)))
                .toList();
    }

    @Override
    public List<TenantPackage> listPackages() {
        return tenantPackageDao.selectList(null).stream()
                .map(this::toPackageEntity)
                .toList();
    }

    @Override
    public TenantPackage findPackageById(String id) {
        TenantPackagePO po = tenantPackageDao.selectById(id);
        return po == null ? null : toPackageEntity(po);
    }

    @Override
    public void savePackage(TenantPackage pkg) {
        tenantPackageDao.insert(toPackagePO(pkg));
    }

    @Override
    public void updatePackage(TenantPackage pkg) {
        tenantPackageDao.updateById(toPackagePO(pkg));
    }

    @Override
    public void deletePackage(String id) {
        tenantPackageDao.deleteById(id);
    }

    private TenantPackagePO toPackagePO(TenantPackage pkg) {
        TenantPackagePO po = new TenantPackagePO();
        po.setId(pkg.getId());
        po.setPackageCode(pkg.getPackageCode());
        po.setPackageName(pkg.getPackageName());
        po.setMaxUsers(pkg.getMaxUsers());
        po.setMaxStorage(pkg.getMaxStorage());
        po.setFeatures(pkg.getFeatures());
        po.setAiEnabled(pkg.getAiEnabled());
        po.setAiQuotaDaily(pkg.getAiQuotaDaily());
        po.setMaxApps(pkg.getMaxApps());
        po.setPrice(pkg.getPrice());
        po.setRemark(pkg.getRemark());
        return po;
    }

    private TenantPO toPO(Tenant entity) {
        TenantPO po = new TenantPO();
        po.setId(entity.getId());
        po.setTenantCode(entity.getTenantCode());
        po.setTenantName(entity.getTenantName());
        po.setContactName(entity.getContactName());
        po.setContactPhone(entity.getContactPhone());
        po.setContactEmail(entity.getContactEmail());
        po.setPackageId(entity.getPackageId());
        po.setStatus(entity.getStatus() == null ? null : entity.getStatus().getCode());
        po.setDomain(entity.getDomain());
        po.setExpireAt(entity.getExpireAt());
        po.setRemark(entity.getRemark());
        return po;
    }

    private Tenant toEntity(TenantPO po) {
        return Tenant.builder()
                .tenantCode(po.getTenantCode())
                .tenantName(po.getTenantName())
                .contactName(po.getContactName())
                .contactPhone(po.getContactPhone())
                .contactEmail(po.getContactEmail())
                .packageId(po.getPackageId())
                .status(TenantStatus.fromCode(po.getStatus()))
                .domain(po.getDomain())
                .expireAt(po.getExpireAt())
                .remark(po.getRemark())
                .id(po.getId())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private TenantPackage toPackageEntity(TenantPackagePO po) {
        return TenantPackage.builder()
                .packageCode(po.getPackageCode())
                .packageName(po.getPackageName())
                .maxUsers(po.getMaxUsers())
                .maxStorage(po.getMaxStorage())
                .features(po.getFeatures())
                .aiEnabled(po.getAiEnabled())
                .aiQuotaDaily(po.getAiQuotaDaily())
                .maxApps(po.getMaxApps())
                .price(po.getPrice())
                .remark(po.getRemark())
                .id(po.getId())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
}
