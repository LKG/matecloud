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
package vip.mate.notice.infrastructure.adapter.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vip.mate.base.result.PageResult;
import vip.mate.notice.domain.adapter.repository.INoticeRepository;
import vip.mate.notice.domain.model.aggregate.NoticeAggregate;
import vip.mate.notice.infrastructure.dao.NoticeDao;
import vip.mate.notice.infrastructure.dao.po.NoticePO;
import vip.mate.notice.types.enums.BusinessType;
import vip.mate.notice.types.enums.NoticeChannel;
import vip.mate.notice.types.enums.NoticeStatus;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class NoticeRepositoryImpl implements INoticeRepository {

    private final NoticeDao noticeDao;

    @Override
    public void save(NoticeAggregate notice) {
        NoticePO po = toPO(notice);
        noticeDao.insert(po);
    }

    @Override
    public void update(NoticeAggregate notice) {
        NoticePO po = toPO(notice);
        noticeDao.updateById(po);
    }

    @Override
    public NoticeAggregate findById(String id) {
        NoticePO po = noticeDao.selectById(id);
        return toDomain(po);
    }

    @Override
    public List<NoticeAggregate> findFailedForRetry(int limit) {
        return noticeDao.selectFailedForRetry(limit).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public PageResult<NoticeAggregate> page(int pageNum, int pageSize,
                                            String channel, Integer status,
                                            String businessType, String keyword) {
        LambdaQueryWrapper<NoticePO> wrapper = new LambdaQueryWrapper<NoticePO>()
                .eq(channel != null && !channel.isBlank(), NoticePO::getChannel, channel)
                .eq(status != null, NoticePO::getStatus, status)
                .eq(businessType != null && !businessType.isBlank(), NoticePO::getBusinessType, businessType)
                .like(keyword != null && !keyword.isBlank(), NoticePO::getTarget, keyword)
                .orderByDesc(NoticePO::getCreatedAt);
        Page<NoticePO> result = noticeDao.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<NoticeAggregate> list = result.getRecords().stream().map(this::toDomain).toList();
        return PageResult.of(list, result.getTotal());
    }

    private NoticePO toPO(NoticeAggregate notice) {
        NoticePO po = new NoticePO();
        po.setId(notice.getId());
        po.setChannel(notice.getChannel() == null ? null : notice.getChannel().getCode());
        po.setTarget(notice.getTarget());
        po.setStatus(notice.getStatus() == null ? null : notice.getStatus().getCode());
        po.setBusinessType(notice.getBusinessType() == null ? null : notice.getBusinessType().getCode());
        po.setContent(notice.getContent());
        po.setResultMessage(notice.getResultMessage());
        po.setSentAt(notice.getSentAt());
        po.setRetryCount(notice.getRetryCount());
        po.setMaxRetries(NoticeAggregate.MAX_RETRY_COUNT);
        return po;
    }

    private NoticeAggregate toDomain(NoticePO po) {
        if (po == null) return null;
        NoticeAggregate aggregate = NoticeAggregate.builder().build();
        aggregate.setId(po.getId());
        aggregate.setChannel(po.getChannel() == null ? null : NoticeChannel.valueOf(po.getChannel()));
        aggregate.setTarget(po.getTarget());
        aggregate.setStatus(NoticeStatus.fromCode(po.getStatus()));
        aggregate.setBusinessType(po.getBusinessType() == null ? null : BusinessType.valueOf(po.getBusinessType()));
        aggregate.setContent(po.getContent());
        aggregate.setResultMessage(po.getResultMessage());
        aggregate.setSentAt(po.getSentAt());
        aggregate.setRetryCount(po.getRetryCount());
        aggregate.setCreatedAt(po.getCreatedAt());
        return aggregate;
    }
}
