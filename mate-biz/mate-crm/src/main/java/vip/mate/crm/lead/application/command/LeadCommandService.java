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
package vip.mate.crm.lead.application.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.mate.base.exception.BizException;
import vip.mate.crm.common.CrmCurrentUser;
import vip.mate.crm.common.CurrentUserResolver;
import vip.mate.crm.customer.application.command.ContactCommand;
import vip.mate.crm.customer.application.command.CustomerCommand;
import vip.mate.crm.customer.application.command.ContactCommandService;
import vip.mate.crm.customer.application.command.CustomerCommandService;
import vip.mate.crm.lead.domain.adapter.repository.ILeadRepository;
import vip.mate.crm.lead.domain.model.aggregate.LeadAggregate;
import vip.mate.crm.types.exception.CrmErrorCode;

import java.time.LocalDateTime;

/**
 * 线索写侧应用服务: 录入/编辑/跟进/转化/废弃/转移编排。转化幂等创建客户 + 主联系人。
 *
 * @author mateaix
 */
@Service
@RequiredArgsConstructor
public class LeadCommandService {

    private final ILeadRepository leadRepository;
    private final CustomerCommandService customerCommandService;
    private final ContactCommandService contactCommandService;
    private final CurrentUserResolver currentUserResolver;

    @Transactional
    public String create(LeadCommand cmd) {
        CrmCurrentUser operator = currentUserResolver.require();
        LeadAggregate agg = LeadAggregate.create(
                cmd.getLeadName(), cmd.getContactName(), cmd.getMobile(), cmd.getTelephone(), cmd.getEmail(),
                cmd.getIndustry(), cmd.getSource(), cmd.getAddress(), cmd.getRemark(), cmd.getNextTime(),
                operator.userId(), operator.displayName(), operator.deptId(), operator.userId(), operator.displayName());
        leadRepository.save(agg);
        return agg.getId();
    }

    @Transactional
    public void update(String id, LeadCommand cmd) {
        LeadAggregate agg = findOrThrow(id);
        agg.updateInfo(cmd.getLeadName(), cmd.getContactName(), cmd.getMobile(), cmd.getTelephone(),
                cmd.getEmail(), cmd.getIndustry(), cmd.getSource(), cmd.getAddress(), cmd.getRemark(), cmd.getNextTime());
        leadRepository.update(agg);
    }

    /** 跟进后更新下次跟进时间(具体跟进记录走跟进上下文)。 */
    @Transactional
    public void touchFollow(String id, LocalDateTime nextTime) {
        LeadAggregate agg = findOrThrow(id);
        agg.touchFollow(nextTime);
        leadRepository.update(agg);
    }

    /** 线索转移。 */
    @Transactional
    public void changeOwner(String id, String ownerId, String ownerName, String deptId) {
        LeadAggregate agg = findOrThrow(id);
        agg.changeOwner(ownerId, ownerName, deptId);
        leadRepository.update(agg);
    }

    /**
     * 转化为客户: 幂等创建私海客户 + 主联系人, 回填 converted_customer_id。
     *
     * @return 新客户ID
     */
    @Transactional
    public String convert(String id) {
        LeadAggregate agg = findOrThrow(id);
        var lead = agg.getLead();

        CustomerCommand customerCmd = new CustomerCommand();
        customerCmd.setCustomerName(lead.getLeadName());
        customerCmd.setIndustry(lead.getIndustry());
        customerCmd.setSource(mapSource(lead.getSource()));
        customerCmd.setPhone(lead.getMobile());
        customerCmd.setAddress(lead.getAddress());
        customerCmd.setRemark(lead.getRemark());
        customerCmd.setToPublicPool(false);
        String customerId = customerCommandService.create(customerCmd);

        ContactCommand contactCmd = new ContactCommand();
        contactCmd.setCustomerId(customerId);
        contactCmd.setCustomerName(lead.getLeadName());
        contactCmd.setContactName(lead.getContactName());
        contactCmd.setMobile(lead.getMobile());
        contactCmd.setTelephone(lead.getTelephone());
        contactCmd.setEmail(lead.getEmail());
        contactCmd.setIsPrimary(1);
        contactCommandService.create(contactCmd);

        agg.markConverted(customerId);
        leadRepository.update(agg);
        return customerId;
    }

    @Transactional
    public void discard(String id) {
        LeadAggregate agg = findOrThrow(id);
        agg.discard();
        leadRepository.update(agg);
    }

    @Transactional
    public void delete(String id) {
        findOrThrow(id);
        leadRepository.deleteById(id);
    }

    private static String mapSource(String leadSource) {
        return leadSource == null || leadSource.isBlank() ? "OTHER" : leadSource;
    }

    private LeadAggregate findOrThrow(String id) {
        LeadAggregate agg = leadRepository.findById(id);
        if (agg == null) {
            throw new BizException(CrmErrorCode.RECORD_NOT_FOUND);
        }
        return agg;
    }
}
