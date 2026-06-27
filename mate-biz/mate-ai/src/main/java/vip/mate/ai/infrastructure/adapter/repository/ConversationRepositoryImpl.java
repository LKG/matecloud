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
package vip.mate.ai.infrastructure.adapter.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vip.mate.ai.domain.adapter.repository.IConversationRepository;
import vip.mate.ai.domain.model.aggregate.ConversationAggregate;
import vip.mate.ai.infrastructure.dao.ConversationDao;
import vip.mate.ai.infrastructure.dao.po.ConversationPO;
import vip.mate.ai.types.enums.ConversationStatus;
import vip.mate.base.result.PageResult;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ConversationRepositoryImpl implements IConversationRepository {

    private final ConversationDao conversationDao;

    @Override
    public void save(ConversationAggregate conversation) {
        conversationDao.insert(toPO(conversation));
    }

    @Override
    public void update(ConversationAggregate conversation) {
        conversationDao.updateById(toPO(conversation));
    }

    @Override
    public void deleteById(String id) {
        conversationDao.deleteById(id);
    }

    @Override
    public ConversationAggregate findById(String id) {
        return toDomain(conversationDao.selectById(id));
    }

    @Override
    public PageResult<ConversationAggregate> pageByOwner(String ownerId, int pageNum, int pageSize,
                                                         Integer status, String keyword) {
        LambdaQueryWrapper<ConversationPO> w = new LambdaQueryWrapper<ConversationPO>()
                .eq(ConversationPO::getOwnerId, ownerId)
                .eq(status != null, ConversationPO::getStatus, status)
                .like(keyword != null && !keyword.isBlank(), ConversationPO::getTitle, keyword)
                .orderByDesc(ConversationPO::getLastActiveAt);
        Page<ConversationPO> result = conversationDao.selectPage(new Page<>(pageNum, pageSize), w);
        List<ConversationAggregate> list = result.getRecords().stream().map(this::toDomain).toList();
        return PageResult.of(list, result.getTotal());
    }

    private ConversationPO toPO(ConversationAggregate c) {
        ConversationPO po = new ConversationPO();
        po.setId(c.getId());
        po.setTenantId(c.getTenantId());
        po.setOwnerId(c.getOwnerId());
        po.setOwnerName(c.getOwnerName());
        po.setTitle(c.getTitle());
        po.setAgentId(c.getAgentId());
        po.setAgentCode(c.getAgentCode());
        po.setProviderId(c.getProviderId());
        po.setModel(c.getModel());
        po.setSystemPrompt(c.getSystemPrompt());
        po.setStatus(c.getStatus() == null ? 0 : c.getStatus().getCode());
        po.setMessageCount(c.getMessageCount() == null ? 0 : c.getMessageCount());
        po.setLastActiveAt(c.getLastActiveAt());
        return po;
    }

    private ConversationAggregate toDomain(ConversationPO po) {
        if (po == null) return null;
        ConversationAggregate c = ConversationAggregate.builder().build();
        c.setId(po.getId());
        c.setTenantId(po.getTenantId());
        c.setOwnerId(po.getOwnerId());
        c.setOwnerName(po.getOwnerName());
        c.setTitle(po.getTitle());
        c.setAgentId(po.getAgentId());
        c.setAgentCode(po.getAgentCode());
        c.setProviderId(po.getProviderId());
        c.setModel(po.getModel());
        c.setSystemPrompt(po.getSystemPrompt());
        c.setStatus(ConversationStatus.fromCode(po.getStatus()));
        c.setMessageCount(po.getMessageCount());
        c.setLastActiveAt(po.getLastActiveAt());
        c.setCreatedAt(po.getCreatedAt());
        c.setUpdatedAt(po.getUpdatedAt());
        return c;
    }
}
