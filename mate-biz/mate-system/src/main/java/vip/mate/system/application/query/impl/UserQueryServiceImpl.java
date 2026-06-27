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
package vip.mate.system.application.query.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vip.mate.api.system.response.UserInfoResponse;
import vip.mate.base.result.PageResult;
import vip.mate.system.application.convertor.UserConvertor;
import vip.mate.system.application.query.IUserQueryService;
import vip.mate.system.domain.adapter.repository.UserRepository;
import vip.mate.system.domain.model.aggregate.UserAggregate;
import vip.mate.system.domain.model.entity.User;
import vip.mate.system.domain.model.valobj.UserStatus;
import vip.mate.system.types.excel.UserExportRow;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserQueryServiceImpl implements IUserQueryService {

    private final UserRepository userRepository;
    private final UserConvertor userConvertor;

    @Override
    public UserInfoResponse findById(String userId) {
        UserAggregate aggregate = userRepository.findById(userId);
        return aggregate == null ? null : userConvertor.toResponse(aggregate);
    }

    @Override
    public UserInfoResponse findByUsername(String username) {
        UserAggregate aggregate = userRepository.findByUsername(username);
        return aggregate == null ? null : userConvertor.toResponse(aggregate);
    }

    @Override
    public UserInfoResponse findByMobile(String mobile) {
        UserAggregate aggregate = userRepository.findByMobile(mobile);
        return aggregate == null ? null : userConvertor.toResponse(aggregate);
    }

    @Override
    public UserInfoResponse findByUsernameForAuth(String username) {
        UserAggregate aggregate = userRepository.findByUsername(username);
        return aggregate == null ? null : userConvertor.toResponseWithPassword(aggregate);
    }

    @Override
    public UserInfoResponse findByMobileForAuth(String mobile) {
        UserAggregate aggregate = userRepository.findByMobile(mobile);
        return aggregate == null ? null : userConvertor.toResponseWithPassword(aggregate);
    }

    @Override
    public PageResult<UserInfoResponse> pageQuery(String keyword, int pageNum, int pageSize) {
        PageResult<UserAggregate> page = userRepository.pageQuery(keyword, pageNum, pageSize);
        List<UserInfoResponse> list = page.getList().stream()
                .map(userConvertor::toResponse)
                .toList();
        return PageResult.of(list, page.getTotal());
    }

    @Override
    public List<UserExportRow> exportRows(String keyword) {
        List<UserAggregate> rows = userRepository.pageQuery(keyword, 1, EXPORT_MAX).getList();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return rows.stream()
                .map(agg -> {
                    User u = agg.getUser();
                    return new UserExportRow(
                            u.getUsername(),
                            u.getRealName(),
                            u.getMobile(),
                            u.getEmail(),
                            statusLabel(u.getStatus()),
                            u.getCreatedAt() == null ? "" : u.getCreatedAt().format(fmt));
                })
                .toList();
    }

    private String statusLabel(UserStatus status) {
        if (status == null) return "";
        return switch (status) {
            case ACTIVE -> "启用";
            case FROZEN -> "冻结";
            case DELETED -> "已删除";
        };
    }
}
