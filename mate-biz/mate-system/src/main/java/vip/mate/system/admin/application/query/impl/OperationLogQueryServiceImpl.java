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
import vip.mate.system.admin.application.query.IOperationLogQueryService;
import vip.mate.system.admin.infrastructure.dao.OperationLogDao;
import vip.mate.system.admin.infrastructure.dao.po.OperationLogPO;
import vip.mate.base.result.PageResult;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OperationLogQueryServiceImpl implements IOperationLogQueryService {

    private final OperationLogDao operationLogDao;

    @Override
    public PageResult<OperationLogVO> page(int pageNum, int pageSize,
                                            String module, String username,
                                            Integer status, String operationType,
                                            LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<OperationLogPO> wrapper = new LambdaQueryWrapper<>();
        if (module != null && !module.isBlank()) {
            wrapper.eq(OperationLogPO::getModule, module);
        }
        if (username != null && !username.isBlank()) {
            wrapper.like(OperationLogPO::getUsername, username);
        }
        if (status != null) {
            wrapper.eq(OperationLogPO::getStatus, status);
        }
        if (operationType != null && !operationType.isBlank()) {
            wrapper.eq(OperationLogPO::getOperationType, operationType);
        }
        if (startTime != null) {
            wrapper.ge(OperationLogPO::getCreatedAt, startTime);
        }
        if (endTime != null) {
            wrapper.le(OperationLogPO::getCreatedAt, endTime);
        }
        wrapper.orderByDesc(OperationLogPO::getCreatedAt);

        Page<OperationLogPO> p = operationLogDao.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<OperationLogVO> rows = p.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(rows, p.getTotal());
    }

    @Override
    public OperationLogDetail findById(String id) {
        OperationLogPO po = operationLogDao.selectById(id);
        return po == null ? null : toDetail(po);
    }

    private OperationLogVO toVO(OperationLogPO po) {
        return new OperationLogVO(po.getId(), po.getUserId(), po.getUsername(),
                po.getModule(), po.getOperationType(), po.getDescription(),
                po.getRequestMethod(), po.getRequestUrl(), po.getClientIp(),
                po.getStatus(), po.getErrorMsg(), po.getDuration(), po.getCreatedAt());
    }

    private OperationLogDetail toDetail(OperationLogPO po) {
        return new OperationLogDetail(po.getId(), po.getUserId(), po.getUsername(),
                po.getModule(), po.getOperationType(), po.getDescription(),
                po.getRequestMethod(), po.getRequestUrl(),
                po.getRequestParams(), po.getResponseResult(),
                po.getClientIp(), po.getUserAgent(), po.getStatus(),
                po.getErrorMsg(), po.getDuration(), po.getCreatedAt());
    }
}
