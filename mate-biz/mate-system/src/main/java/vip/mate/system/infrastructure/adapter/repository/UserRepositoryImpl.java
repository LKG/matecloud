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
package vip.mate.system.infrastructure.adapter.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vip.mate.base.result.PageResult;
import vip.mate.system.domain.adapter.repository.UserRepository;
import vip.mate.system.domain.model.aggregate.UserAggregate;
import vip.mate.system.domain.model.entity.User;
import vip.mate.system.domain.model.entity.UserOperateStream;
import vip.mate.system.infrastructure.adapter.repository.convertor.UserInfraConvertor;
import vip.mate.system.infrastructure.dao.UserDao;
import vip.mate.system.infrastructure.dao.UserOperateStreamDao;
import vip.mate.system.infrastructure.dao.po.UserOperateStreamPO;
import vip.mate.system.infrastructure.dao.po.UserPO;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserDao userDao;
    private final UserOperateStreamDao userOperateStreamDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(UserAggregate aggregate) {
        UserPO po = UserInfraConvertor.INSTANCE.toPO(aggregate.getUser());
        userDao.insert(po);
        aggregate.getUser().setId(po.getId());
        batchSaveStreams(aggregate.getAndClearOperateStreams());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(UserAggregate aggregate) {
        UserPO po = UserInfraConvertor.INSTANCE.toPO(aggregate.getUser());
        userDao.updateById(po);
        batchSaveStreams(aggregate.getAndClearOperateStreams());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        // MyBatis-Plus @TableLogic → UPDATE mate_user SET deleted=1 WHERE id=?
        userDao.deleteById(id);
    }

    @Override
    public UserAggregate findById(String id) {
        UserPO po = userDao.selectById(id);
        return toAggregate(po);
    }

    @Override
    public UserAggregate findByMobile(String mobile) {
        UserPO po = userDao.selectByMobile(mobile);
        return toAggregate(po);
    }

    @Override
    public UserAggregate findByUsername(String username) {
        UserPO po = userDao.selectByUsername(username);
        return toAggregate(po);
    }

    @Override
    public boolean existsByMobile(String mobile) {
        return mobile != null && userDao.countByMobile(mobile) > 0;
    }

    @Override
    public boolean existsByUsername(String username) {
        return username != null && userDao.countByUsername(username) > 0;
    }

    @Override
    public PageResult<UserAggregate> pageQuery(String keyword, int pageNum, int pageSize) {
        Page<UserPO> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        // deleted=0 is also auto-appended by @TableLogic; kept explicit for clarity.
        wrapper.eq(UserPO::getDeleted, 0);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w
                    .like(UserPO::getUsername, keyword)
                    .or().like(UserPO::getRealName, keyword)
                    .or().like(UserPO::getMobile, keyword));
        }
        wrapper.orderByDesc(UserPO::getCreatedAt);
        Page<UserPO> result = userDao.selectPage(page, wrapper);
        List<UserAggregate> list = result.getRecords().stream()
                .map(po -> UserAggregate.create(UserInfraConvertor.INSTANCE.toEntity(po)))
                .toList();
        return PageResult.of(list, result.getTotal());
    }

    private UserAggregate toAggregate(UserPO po) {
        if (po == null) return null;
        User user = UserInfraConvertor.INSTANCE.toEntity(po);
        return UserAggregate.create(user);
    }

    private void batchSaveStreams(List<UserOperateStream> streams) {
        if (streams == null || streams.isEmpty()) return;
        for (UserOperateStream stream : streams) {
            UserOperateStreamPO po = UserInfraConvertor.INSTANCE.toStreamPO(stream);
            userOperateStreamDao.insert(po);
        }
    }
}
