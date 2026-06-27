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
package vip.mate.system.admin.infrastructure.datascope;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;
import vip.mate.system.admin.domain.permission.adapter.repository.AdminRepository;
import vip.mate.system.admin.domain.permission.adapter.repository.RoleRepository;
import vip.mate.system.admin.domain.permission.model.aggregate.AdminAggregate;
import vip.mate.system.admin.domain.permission.model.aggregate.RoleAggregate;
import vip.mate.system.admin.domain.permission.model.entity.Role;

import java.util.ArrayList;
import java.util.List;

/**
 * Populates the Sa-Token session with the current admin's effective data scope
 * — the keys {@code dataScope} / {@code deptId} / {@code customDeptIds} that
 * {@code DataScopeAspect} (security-starter) reads before each
 * {@code @DataPermission} query.
 *
 * <p>Resolved once per session (cached in the session) from the admin's roles:
 * the effective scope is the <b>broadest</b> among the admin's roles (lowest
 * code, ALL=1 wins), since multiple roles grant the union of visible data.
 * Falls back to ALL (no filtering) when the principal has no admin record or
 * no roles — preserving prior behaviour.
 *
 * @author mateaix
 */
@Slf4j
@RequiredArgsConstructor
public class DataScopeSessionInterceptor implements HandlerInterceptor {

    private static final int SCOPE_ALL = 1;
    private static final int SCOPE_CUSTOM = 5;

    private final AdminRepository adminRepository;
    private final RoleRepository roleRepository;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        try {
            if (StpUtil.getLoginIdDefaultNull() == null) {
                return true;
            }
            var session = StpUtil.getSession();
            if (session.get("dataScope") != null) {
                return true; // already resolved for this session
            }
            Object username = session.get("username");
            if (username == null) {
                return true;
            }
            AdminAggregate agg = adminRepository.findByUsername(username.toString());
            if (agg == null) {
                return true; // not an admin principal → leave scope unset (ALL)
            }
            resolveAndStore(agg, session);
        } catch (Exception e) {
            log.warn("[datascope] failed to resolve data scope: {}", e.getMessage());
        }
        return true;
    }

    private void resolveAndStore(AdminAggregate agg, cn.dev33.satoken.session.SaSession session) {
        int effective = SCOPE_ALL;
        boolean any = false;
        List<String> customIds = new ArrayList<>();
        for (String roleId : agg.getRoleIds()) {
            RoleAggregate roleAgg = roleRepository.findById(roleId);
            if (roleAgg == null) {
                continue;
            }
            Role role = roleAgg.getRole();
            int code = role.getDataScope() == null ? SCOPE_ALL : role.getDataScope();
            if (!any || code < effective) {
                effective = code;
                any = true;
            }
            if (code == SCOPE_CUSTOM && role.getCustomDeptIds() != null && !role.getCustomDeptIds().isBlank()) {
                customIds.add(role.getCustomDeptIds());
            }
        }
        session.set("dataScope", effective);
        session.set("deptId", agg.getAdmin().getDeptId());
        if (effective == SCOPE_CUSTOM && !customIds.isEmpty()) {
            session.set("customDeptIds", String.join(",", customIds));
        }
    }
}
