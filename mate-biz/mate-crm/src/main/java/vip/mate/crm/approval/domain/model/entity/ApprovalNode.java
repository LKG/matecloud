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
package vip.mate.crm.approval.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审批节点(值对象): 节点名称 + 审批人。序列化进 mate_crm_approval.nodes_json 快照。
 *
 * @author mateaix
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalNode {

    private String nodeName;
    private String approverId;
    private String approverName;

    public static ApprovalNode of(String nodeName, String approverId, String approverName) {
        return new ApprovalNode(nodeName, approverId, approverName);
    }
}
