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
package vip.mate.starter.sso.core.sync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import vip.mate.base.exception.BizException;
import vip.mate.starter.sso.config.SsoProperties;
import vip.mate.starter.sso.core.ProviderRegistry;
import vip.mate.starter.sso.core.SsoConfigStore;
import vip.mate.starter.sso.event.OrgSyncCompletedEvent;
import vip.mate.starter.sso.event.OrgSyncStartedEvent;
import vip.mate.starter.sso.port.IdentityMappingPort;
import vip.mate.starter.sso.port.OrgProvisionPort;
import vip.mate.starter.sso.port.ProviderRef;
import vip.mate.starter.sso.spi.IdentityProvider;
import vip.mate.starter.sso.spi.model.ExternalDept;
import vip.mate.starter.sso.spi.model.ExternalUser;
import vip.mate.starter.sso.spi.model.ProviderConfig;
import vip.mate.starter.sso.types.SsoErrorCode;

/**
 * Organization sync as a pipeline: <em>lock → fetch → build dept tree → provision
 * depts → provision active users → prune left users/depts → publish</em>. Vendor
 * differences live behind {@link IdentityProvider}; business persistence behind
 * {@link OrgProvisionPort} (hexagonal).
 *
 * @author mateaix
 */
public class SyncPipeline {

    private static final Logger log = LoggerFactory.getLogger(SyncPipeline.class);

    private final ProviderRegistry registry;
    private final SsoConfigStore configStore;
    private final IdentityMappingPort mappingPort;
    private final ObjectProvider<OrgProvisionPort> provisionPort;
    private final SyncLock lock;
    private final ApplicationEventPublisher events;
    private final SsoProperties properties;

    public SyncPipeline(ProviderRegistry registry,
                        SsoConfigStore configStore,
                        IdentityMappingPort mappingPort,
                        ObjectProvider<OrgProvisionPort> provisionPort,
                        SyncLock lock,
                        ApplicationEventPublisher events,
                        SsoProperties properties) {
        this.registry = registry;
        this.configStore = configStore;
        this.mappingPort = mappingPort;
        this.provisionPort = provisionPort;
        this.lock = lock;
        this.events = events;
        this.properties = properties;
    }

    public SyncResult run(String providerCode, SyncMode mode) {
        return lock.runExclusive("sso:sync:" + providerCode, () -> doRun(providerCode, mode));
    }

    private SyncResult doRun(String providerCode, SyncMode mode) {
        OrgProvisionPort provision = provisionPort.getIfAvailable();
        if (provision == null) {
            throw new BizException(SsoErrorCode.SSO_C_PROVIDER_DISABLED.getCode(),
                    "OrgProvisionPort not implemented — organization sync has nowhere to land. "
                            + "The consuming service must provide one.");
        }
        IdentityProvider provider = registry.get(providerCode);
        ProviderConfig cfg = configStore.config(providerCode);
        ProviderRef ref = ProviderRef.of(providerCode);

        events.publishEvent(new OrgSyncStartedEvent(this, providerCode, mode));
        try {
            // 1) fetch
            List<ExternalDept> depts = provider.fetchDepartments(cfg);
            List<ExternalUser> allUsers = provider.fetchUsers(cfg, depts);

            // 2) provision dept tree top-down so parents resolve before children
            Map<String, String> extToLocalDept = provisionDeptTree(depts, provision, ref);

            // 3) provision ACTIVE users + bind identity mapping (skip disabled/left)
            int added = 0;
            int updated = 0;
            Set<String> seenUserExtIds = new HashSet<>();
            for (ExternalUser u : allUsers) {
                if (!u.enabled()) {
                    continue; // status != active → treated as "not present", pruned below
                }
                seenUserExtIds.add(u.externalId());
                List<String> localDeptIds = new ArrayList<>();
                for (String extDeptId : nullSafe(u.deptExternalIds())) {
                    String localId = extToLocalDept.get(extDeptId);
                    if (localId != null) {
                        localDeptIds.add(localId);
                    }
                }
                String existingUserId = mappingPort.resolveUser(providerCode, u.externalId(), u.unionId()).orElse(null);
                String localUserId = provision.provisionUser(u, localDeptIds, existingUserId, ref);
                mappingPort.bindUser(providerCode, u.externalId(), u.unionId(), localUserId);
                if (existingUserId != null) {
                    updated++;
                } else {
                    added++;
                }
            }

            // 4) prune (left org) — only on FULL sync
            int removed = 0;
            if (mode == SyncMode.FULL) {
                removed = pruneUsers(providerCode, seenUserExtIds, provision, ref);
                pruneDepts(providerCode, extToLocalDept.keySet(), provision, ref);
            }

            SyncResult r = SyncResult.ok(providerCode, depts.size(), added, updated, removed);
            events.publishEvent(new OrgSyncCompletedEvent(this, r));
            return r;
        } catch (Exception e) {
            log.error("SSO org sync failed for provider {}", providerCode, e);
            SyncResult r = SyncResult.fail(providerCode, e.getMessage());
            events.publishEvent(new OrgSyncCompletedEvent(this, r));
            return r;
        }
    }

    /** Disable or unlink local users whose external id was not seen this run. */
    private int pruneUsers(String providerCode, Set<String> seenExtIds,
                           OrgProvisionPort provision, ProviderRef ref) {
        int removed = 0;
        for (IdentityMappingPort.UserMapping m : mappingPort.listUserMappings(providerCode)) {
            if (seenExtIds.contains(m.externalId()) || m.principalId() == null) {
                continue;
            }
            if (properties.getSync().getLeaveStrategy() == LeaveStrategy.UNLINK) {
                provision.unlinkUser(m.principalId(), ref);
                mappingPort.unbindUser(providerCode, m.externalId());
            } else {
                provision.disableUser(m.principalId(), ref);
            }
            removed++;
        }
        return removed;
    }

    /** Remove local departments whose external id was not seen this run. */
    private void pruneDepts(String providerCode, Set<String> seenExtIds,
                            OrgProvisionPort provision, ProviderRef ref) {
        for (IdentityMappingPort.DeptMapping m : mappingPort.listDeptMappings(providerCode)) {
            if (seenExtIds.contains(m.externalId()) || m.localDeptId() == null) {
                continue;
            }
            provision.removeDept(m.localDeptId(), ref);
            mappingPort.unbindDept(providerCode, m.externalId());
        }
    }

    /** Upsert departments parent-before-child; returns externalId → localDeptId. */
    private Map<String, String> provisionDeptTree(List<ExternalDept> depts,
                                                OrgProvisionPort provision,
                                                ProviderRef ref) {
        Map<String, ExternalDept> byExt = new HashMap<>();
        for (ExternalDept d : depts) {
            byExt.put(d.externalId(), d);
        }
        Map<String, String> resolved = new HashMap<>();
        for (ExternalDept d : depts) {
            resolveDept(d, byExt, resolved, provision, ref);
        }
        return resolved;
    }

    private String resolveDept(ExternalDept d,
                             Map<String, ExternalDept> byExt,
                             Map<String, String> resolved,
                             OrgProvisionPort provision,
                             ProviderRef ref) {
        if (d == null) {
            return null;
        }
        String cached = resolved.get(d.externalId());
        if (cached != null) {
            return cached;
        }
        String parentLocalId = null;
        String parentExt = d.parentExternalId();
        if (parentExt != null && !parentExt.isBlank() && !"0".equals(parentExt)) {
            parentLocalId = resolveDept(byExt.get(parentExt), byExt, resolved, provision, ref);
        }
        String existingLocalId = mappingPort.resolveDept(ref.provider(), d.externalId()).orElse(null);
        String localId = provision.upsertDept(d, parentLocalId, existingLocalId, ref);
        resolved.put(d.externalId(), localId);
        mappingPort.bindDept(ref.provider(), d.externalId(), localId);
        return localId;
    }

    private static <T> List<T> nullSafe(List<T> in) {
        return in == null ? List.of() : in;
    }
}
