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
package vip.mate.system.admin.application.query.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vip.mate.api.system.enums.UserStatus;
import vip.mate.api.system.response.UserInfoResponse;
import vip.mate.base.result.PageResult;
import vip.mate.starter.datascope.DataPermission;
import vip.mate.system.admin.application.query.IAdminQueryService;
import vip.mate.system.admin.domain.permission.adapter.repository.AdminRepository;
import vip.mate.system.admin.domain.permission.model.aggregate.AdminAggregate;
import vip.mate.system.admin.domain.permission.model.entity.Admin;
import vip.mate.system.admin.domain.permission.model.valobj.AdminStatus;
import vip.mate.system.admin.infrastructure.dao.AdminDao;
import vip.mate.system.admin.infrastructure.dao.po.AdminPO;
import vip.mate.system.admin.types.excel.AdminExportRow;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminQueryServiceImpl implements IAdminQueryService {

    private final AdminRepository adminRepository;
    private final AdminDao adminDao;

    // ---- Authentication lookups: build straight from the PO so the password
    // hash + status survive (the domain aggregate intentionally hides them). ----
    @Override
    public UserInfoResponse findForAuthByUsername(String username) {
        return toAuthResponse(adminDao.selectByUsername(username));
    }

    @Override
    public UserInfoResponse findForAuthByMobile(String mobile) {
        return toAuthResponse(adminDao.selectByMobile(mobile));
    }

    @Override
    public UserInfoResponse findForAuthById(String id) {
        return toAuthResponse(adminDao.selectById(id));
    }

    private UserInfoResponse toAuthResponse(AdminPO po) {
        if (po == null) return null;
        return UserInfoResponse.builder()
                .userId(po.getId())
                .username(po.getUsername())
                .realName(po.getRealName() != null ? po.getRealName() : po.getNickName())
                .mobile(po.getMobile())
                .email(po.getEmail())
                .avatar(po.getAvatar())
                .password(po.getPassword())
                .status(UserStatus.fromCode(po.getStatus()))
                .tenantId(po.getTenantId())
                .build();
    }

    @Override
    public AdminVO findById(String id) {
        AdminAggregate agg = adminRepository.findById(id);
        return agg == null ? null : toVO(agg);
    }

    @Override
    @DataPermission(includeTables = {"mate_admin"}, deptAlias = "dept_id", userAlias = "id")
    public PageResult<AdminVO> pageQuery(String keyword, int pageNum, int pageSize) {
        PageResult<AdminAggregate> page = adminRepository.pageQuery(keyword, pageNum, pageSize);
        List<AdminVO> list = page.getList().stream().map(this::toVO).toList();
        return PageResult.of(list, page.getTotal());
    }

    @Override
    public List<AdminExportRow> exportRows() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return adminRepository.pageQuery(null, 1, EXPORT_MAX).getList().stream()
                .map(agg -> {
                    Admin a = agg.getAdmin();
                    return new AdminExportRow(
                            a.getUsername(),
                            a.getNickName(),
                            statusLabel(a.getStatus()),
                            a.getCreatedAt() == null ? "" : a.getCreatedAt().format(fmt));
                })
                .toList();
    }

    private String statusLabel(AdminStatus status) {
        if (status == null) return "";
        return switch (status) {
            case ACTIVE -> "启用";
            case DISABLED -> "停用";
        };
    }

    private AdminVO toVO(AdminAggregate agg) {
        Admin a = agg.getAdmin();
        return new AdminVO(a.getId(), a.getUsername(), a.getMobile(), a.getEmail(),
                a.getNickName(), a.getRealName(), a.getAvatar(),
                a.getStatus() != null ? a.getStatus().name() : null,
                a.getDeptId(), agg.getRoleIds(), a.getCreatedAt());
    }
}
