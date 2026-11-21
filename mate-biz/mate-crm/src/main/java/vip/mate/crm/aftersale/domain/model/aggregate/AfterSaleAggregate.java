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
package vip.mate.crm.aftersale.domain.model.aggregate;

import lombok.Builder;
import lombok.Getter;
import vip.mate.base.exception.BizException;
import vip.mate.base.response.ResponseCode;
import vip.mate.crm.aftersale.domain.model.entity.AfterSale;
import vip.mate.crm.aftersale.domain.model.entity.AfterSaleLog;
import vip.mate.crm.aftersale.types.enums.AfterSaleAction;
import vip.mate.crm.aftersale.types.enums.AfterSaleStatus;
import vip.mate.crm.common.CrmCurrentUser;
import vip.mate.crm.types.exception.CrmErrorCode;
import vip.mate.starter.distribute.util.SnowflakeUtil;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 售后工单聚合根: 待受理 → 处理中 → 已解决 →(评价)已关闭。
 *
 * <p>不变式: 受理仅限待受理态; 处理记录仅限处理中态; 解决需处理中态; 评价仅限已解决态且评价即关闭。
 * 每次状态迁移追加一条处理日志(时间线)。
 *
 * @author mateaix
 */
@Getter
@Builder
public class AfterSaleAggregate {

    private final AfterSale afterSale;

    @Builder.Default
    private final List<AfterSaleLog> logs = new ArrayList<>();

    public static AfterSaleAggregate create(String title, String customerId, String customerName,
                                            String contractId, String type, String priority,
                                            String description, CrmCurrentUser operator) {
        requireText(title, "工单标题不能为空");
        if (customerId == null || customerId.isBlank()) {
            throw new BizException(ResponseCode.PARAM_VALID_ERROR.getCode(), "工单必须关联客户");
        }
        AfterSale afterSale = AfterSale.builder()
                .ticketNo("SH" + SnowflakeUtil.newSnowflakeId())
                .title(title)
                .customerId(customerId)
                .customerName(customerName)
                .contractId(contractId)
                .type(type == null || type.isBlank() ? "SERVICE" : type)
                .priority(priority == null || priority.isBlank() ? "MEDIUM" : priority)
                .status(AfterSaleStatus.OPEN.name())
                .description(description)
                .ownerId(operator.userId())
                .ownerName(operator.displayName())
                .deptId(operator.deptId())
                .creatorId(operator.userId())
                .creatorName(operator.displayName())
                .build();
        afterSale.setId(SnowflakeUtil.newSnowflakeId());
        AfterSaleAggregate agg = AfterSaleAggregate.builder().afterSale(afterSale).build();
        agg.appendLog(AfterSaleAction.CREATE, "创建工单", operator);
        return agg;
    }

    public static AfterSaleAggregate of(AfterSale afterSale, List<AfterSaleLog> logs) {
        if (afterSale == null) {
            throw new BizException(CrmErrorCode.RECORD_NOT_FOUND);
        }
        List<AfterSaleLog> list = logs == null ? new ArrayList<>() : new ArrayList<>(logs);
        return AfterSaleAggregate.builder().afterSale(afterSale).logs(list).build();
    }

    /** 受理: 待受理 → 处理中, 指派处理人。 */
    public void accept(CrmCurrentUser operator) {
        assertNotClosed();
        if (AfterSaleStatus.of(afterSale.getStatus()) != AfterSaleStatus.OPEN) {
            throw new BizException(CrmErrorCode.TICKET_NOT_OPEN);
        }
        afterSale.setStatus(AfterSaleStatus.PROCESSING.name());
        afterSale.setHandlerId(operator.userId());
        afterSale.setHandlerName(operator.displayName());
        appendLog(AfterSaleAction.ACCEPT, "受理工单", operator);
    }

    /** 处理: 追加处理记录(处理中态)。 */
    public void process(String content, CrmCurrentUser operator) {
        assertNotClosed();
        if (AfterSaleStatus.of(afterSale.getStatus()) != AfterSaleStatus.PROCESSING) {
            throw new BizException(CrmErrorCode.TICKET_NOT_OPEN.getCode(), "请先受理工单再记录处理");
        }
        appendLog(AfterSaleAction.PROCESS, content, operator);
    }

    /** 完成: 处理中 → 已解决。 */
    public void resolve(String content, CrmCurrentUser operator) {
        assertNotClosed();
        if (AfterSaleStatus.of(afterSale.getStatus()) != AfterSaleStatus.PROCESSING) {
            throw new BizException(CrmErrorCode.TICKET_NOT_OPEN.getCode(), "工单需处于处理中才能完成");
        }
        afterSale.setStatus(AfterSaleStatus.RESOLVED.name());
        afterSale.setResolveTime(LocalDateTime.now());
        appendLog(AfterSaleAction.FINISH, content, operator);
    }

    /** 关闭: 未关闭工单可手动关闭。 */
    public void close(String content, CrmCurrentUser operator) {
        assertNotClosed();
        afterSale.setStatus(AfterSaleStatus.CLOSED.name());
        afterSale.setCloseTime(LocalDateTime.now());
        appendLog(AfterSaleAction.CLOSE, content, operator);
    }

    /** 评价: 仅限已解决工单, 评价后关闭。 */
    public void rate(Integer rating, String remark, CrmCurrentUser operator) {
        assertNotClosed();
        if (AfterSaleStatus.of(afterSale.getStatus()) != AfterSaleStatus.RESOLVED) {
            throw new BizException(CrmErrorCode.TICKET_NOT_RESOLVED);
        }
        if (rating == null || rating < 1 || rating > 5) {
            throw new BizException(ResponseCode.PARAM_VALID_ERROR.getCode(), "满意度评分需 1-5");
        }
        afterSale.setRating(rating);
        afterSale.setRatingRemark(remark);
        afterSale.setStatus(AfterSaleStatus.CLOSED.name());
        afterSale.setCloseTime(LocalDateTime.now());
        appendLog(AfterSaleAction.RATE, "满意度评价: " + rating + (remark == null ? "" : " " + remark), operator);
    }

    public String getId() {
        return afterSale.getId();
    }

    private void appendLog(AfterSaleAction action, String content, CrmCurrentUser operator) {
        AfterSaleLog log = AfterSaleLog.builder()
                .id(SnowflakeUtil.newSnowflakeId())
                .afterSaleId(afterSale.getId())
                .action(action.name())
                .content(content)
                .operatorId(operator.userId())
                .operatorName(operator.displayName())
                .build();
        logs.add(log);
    }

    private void assertNotClosed() {
        if (AfterSaleStatus.of(afterSale.getStatus()).isClosed()) {
            throw new BizException(CrmErrorCode.TICKET_CLOSED);
        }
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BizException(ResponseCode.PARAM_VALID_ERROR.getCode(), message);
        }
    }
}
