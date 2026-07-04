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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vip.mate.system.admin.application.query.ILoginLogQueryService;
import vip.mate.system.admin.infrastructure.dao.LoginLogDao;
import vip.mate.system.admin.infrastructure.dao.po.LoginLogPO;
import vip.mate.base.result.PageResult;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoginLogQueryServiceImpl implements ILoginLogQueryService {

    private final LoginLogDao loginLogDao;

    @Override
    public PageResult<LoginLogVO> page(int pageNum, int pageSize,
                                        String username, Integer status, String loginType,
                                        LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<LoginLogPO> wrapper = new LambdaQueryWrapper<>();
        if (username != null && !username.isBlank()) {
            wrapper.like(LoginLogPO::getUsername, username);
        }
        if (status != null) {
            wrapper.eq(LoginLogPO::getStatus, status);
        }
        if (loginType != null && !loginType.isBlank()) {
            wrapper.eq(LoginLogPO::getLoginType, loginType);
        }
        if (startTime != null) {
            wrapper.ge(LoginLogPO::getCreatedAt, startTime);
        }
        if (endTime != null) {
            wrapper.le(LoginLogPO::getCreatedAt, endTime);
        }
        wrapper.orderByDesc(LoginLogPO::getCreatedAt);

        Page<LoginLogPO> p = loginLogDao.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<LoginLogVO> rows = p.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(rows, p.getTotal());
    }

    @Override
    public LoginLogVO findById(String id) {
        LoginLogPO po = loginLogDao.selectById(id);
        return po == null ? null : toVO(po);
    }

    private LoginLogVO toVO(LoginLogPO po) {
        return new LoginLogVO(po.getId(), po.getUsername(), po.getClientIp(),
                po.getUserAgent(), po.getLoginType(), po.getStatus(),
                po.getFailMsg(), po.getCreatedAt());
    }
}
