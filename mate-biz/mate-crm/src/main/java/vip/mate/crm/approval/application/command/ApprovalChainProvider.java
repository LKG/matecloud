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
package vip.mate.crm.approval.application.command;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vip.mate.crm.approval.domain.model.entity.ApprovalNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 审批链解析器: 合同默认两级(部门主管 → 财务), 两级可配置。
 *
 * <p>approver-id 留空表示「不校验具体审批人」(由 {@code crm:approval:audit} 权限把关, 单体演示友好);
 * 生产可配置为具体用户ID以强制节点审批人校验。后续可平移到 mate-flow-starter 编排。
 *
 * @author mateaix
 */
@Component
public class ApprovalChainProvider {

    @Value("${mate.crm.approval.contract.node1-name:部门主管}")
    private String node1Name;

    @Value("${mate.crm.approval.contract.node1-id:}")
    private String node1Id;

    @Value("${mate.crm.approval.contract.node2-name:财务}")
    private String node2Name;

    @Value("${mate.crm.approval.contract.node2-id:}")
    private String node2Id;

    /** 合同审批两级链。 */
    public List<ApprovalNode> contractChain() {
        List<ApprovalNode> chain = new ArrayList<>();
        chain.add(node(node1Name, node1Id));
        chain.add(node(node2Name, node2Id));
        return chain;
    }

    private ApprovalNode node(String name, String id) {
        return ApprovalNode.of(name, blankToNull(id), null);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
