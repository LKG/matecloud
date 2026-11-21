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
package vip.mate.crm.lead.domain.model.valobj;

import vip.mate.crm.common.CrmScope;

/**
 * 线索分页查询条件(含数据权限快照)。
 *
 * @param keyword  名称/联系人/手机号模糊词
 * @param status   FOLLOWING/CONVERTED/INVALID, 空=全部
 * @param source   来源过滤, 空=全部
 * @param ownerId  负责人过滤, 空=全部(「我的线索」传当前用户)
 * @param scope    数据权限范围
 * @param pageNum  页码(1起)
 * @param pageSize 每页条数
 */
public record LeadQuery(String keyword, String status, String source, String ownerId,
                        CrmScope scope, int pageNum, int pageSize) {
}
