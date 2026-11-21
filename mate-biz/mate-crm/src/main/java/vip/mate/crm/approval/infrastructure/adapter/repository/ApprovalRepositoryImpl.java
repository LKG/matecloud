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
package vip.mate.crm.approval.infrastructure.adapter.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Repository;
import vip.mate.base.result.PageResult;
import vip.mate.crm.approval.domain.adapter.repository.IApprovalRepository;
import vip.mate.crm.approval.domain.model.aggregate.ApprovalAggregate;
import vip.mate.crm.approval.domain.model.entity.Approval;
import vip.mate.crm.approval.domain.model.entity.ApprovalNode;
import vip.mate.crm.approval.domain.model.entity.ApprovalRecord;
import vip.mate.crm.approval.infrastructure.dao.ApprovalDao;
import vip.mate.crm.approval.infrastructure.dao.ApprovalRecordDao;
import vip.mate.crm.approval.infrastructure.dao.po.ApprovalPO;
import vip.mate.crm.approval.infrastructure.dao.po.ApprovalRecordPO;
import vip.mate.crm.approval.types.enums.ApprovalStatus;
import vip.mate.crm.common.CrmScope;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 审批实例仓储实现: 主表(nodes_json 快照) + 操作记录(仅追加, 主表更新只补插未落库的新记录)。 */
@Repository
@RequiredArgsConstructor
public class ApprovalRepositoryImpl implements IApprovalRepository {

    private final ApprovalDao approvalDao;
    private final ApprovalRecordDao approvalRecordDao;
    private final ObjectMapper objectMapper;

    @Override
    public void save(ApprovalAggregate aggregate) {
        approvalDao.insert(toPO(aggregate));
        saveRecords(aggregate.getRecords());
    }

    @Override
    public void update(ApprovalAggregate aggregate) {
        approvalDao.updateById(toPO(aggregate));
        // 操作记录仅追加: 删后重插会与 @TableLogic 软删行冲突(主键重复), 且审计语义要求不丢历史
        Set<String> existing = loadRecords(aggregate.getId()).stream()
                .map(ApprovalRecord::getId).collect(Collectors.toSet());
        List<ApprovalRecord> fresh = aggregate.getRecords().stream()
                .filter(r -> !existing.contains(r.getId())).toList();
        saveRecords(fresh);
    }

    @Override
    public ApprovalAggregate findById(String id) {
        ApprovalPO po = approvalDao.selectById(id);
        if (po == null) {
            return null;
        }
        return ApprovalAggregate.of(toEntity(po), parseNodes(po.getNodesJson()), loadRecords(id));
    }

    @Override
    public void deleteById(String id) {
        approvalDao.deleteById(id);
        LambdaQueryWrapper<ApprovalRecordPO> del = new LambdaQueryWrapper<>();
        del.eq(ApprovalRecordPO::getApprovalId, id);
        approvalRecordDao.delete(del);
    }

    @Override
    public PageResult<Approval> page(String keyword, String status, String bizType, String applicantId,
                                     CrmScope scope, int pageNum, int pageSize) {
        LambdaQueryWrapper<ApprovalPO> wrapper = new LambdaQueryWrapper<>();
        if (notBlank(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(ApprovalPO::getTitle, kw)
                    .or().like(ApprovalPO::getBizNo, kw));
        }
        wrapper.eq(notBlank(status), ApprovalPO::getStatus, status)
                .eq(notBlank(bizType), ApprovalPO::getBizType, bizType)
                .eq(notBlank(applicantId), ApprovalPO::getApplicantId, applicantId);
        (scope == null ? CrmScope.all() : scope).apply(wrapper, ApprovalPO::getApplicantId, ApprovalPO::getDeptId);
        wrapper.orderByDesc(ApprovalPO::getUpdatedAt);

        Page<ApprovalPO> result = approvalDao.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<Approval> list = result.getRecords().stream().map(this::toEntity).toList();
        return PageResult.of(list, result.getTotal());
    }

    @Override
    public boolean existsPending(String bizType, String bizId) {
        LambdaQueryWrapper<ApprovalPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalPO::getBizType, bizType)
                .eq(ApprovalPO::getBizId, bizId)
                .eq(ApprovalPO::getStatus, ApprovalStatus.PENDING.name());
        return approvalDao.selectCount(wrapper) > 0;
    }

    private void saveRecords(List<ApprovalRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        for (ApprovalRecord r : records) {
            approvalRecordDao.insert(toRecordPO(r));
        }
    }

    private List<ApprovalRecord> loadRecords(String approvalId) {
        LambdaQueryWrapper<ApprovalRecordPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalRecordPO::getApprovalId, approvalId).orderByAsc(ApprovalRecordPO::getActedAt);
        return approvalRecordDao.selectList(wrapper).stream().map(this::toRecordEntity).toList();
    }

    @SneakyThrows
    private String writeNodes(List<ApprovalNode> nodes) {
        List<ApprovalNode> list = nodes == null ? new ArrayList<>() : nodes;
        return objectMapper.writeValueAsString(list);
    }

    @SneakyThrows
    private List<ApprovalNode> parseNodes(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        return objectMapper.readValue(json, new TypeReference<List<ApprovalNode>>() {
        });
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private ApprovalPO toPO(ApprovalAggregate aggregate) {
        Approval a = aggregate.getApproval();
        ApprovalPO po = new ApprovalPO();
        po.setId(a.getId());
        po.setBizType(a.getBizType());
        po.setBizId(a.getBizId());
        po.setBizNo(a.getBizNo());
        po.setTitle(a.getTitle());
        po.setApplicantId(a.getApplicantId());
        po.setApplicantName(a.getApplicantName());
        po.setDeptId(a.getDeptId());
        po.setStatus(a.getStatus());
        po.setCurrentNode(a.getCurrentNode());
        po.setNodesJson(writeNodes(aggregate.getNodes()));
        return po;
    }

    private Approval toEntity(ApprovalPO po) {
        return Approval.builder()
                .id(po.getId())
                .bizType(po.getBizType())
                .bizId(po.getBizId())
                .bizNo(po.getBizNo())
                .title(po.getTitle())
                .applicantId(po.getApplicantId())
                .applicantName(po.getApplicantName())
                .deptId(po.getDeptId())
                .status(po.getStatus())
                .currentNode(po.getCurrentNode())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private ApprovalRecordPO toRecordPO(ApprovalRecord r) {
        ApprovalRecordPO po = new ApprovalRecordPO();
        po.setId(r.getId());
        po.setApprovalId(r.getApprovalId());
        po.setNodeIndex(r.getNodeIndex());
        po.setNodeName(r.getNodeName());
        po.setApproverId(r.getApproverId());
        po.setApproverName(r.getApproverName());
        po.setAction(r.getAction());
        po.setRemark(r.getRemark());
        po.setActedAt(r.getActedAt());
        return po;
    }

    private ApprovalRecord toRecordEntity(ApprovalRecordPO po) {
        return ApprovalRecord.builder()
                .id(po.getId())
                .approvalId(po.getApprovalId())
                .nodeIndex(po.getNodeIndex())
                .nodeName(po.getNodeName())
                .approverId(po.getApproverId())
                .approverName(po.getApproverName())
                .action(po.getAction())
                .remark(po.getRemark())
                .actedAt(po.getActedAt())
                .build();
    }
}
