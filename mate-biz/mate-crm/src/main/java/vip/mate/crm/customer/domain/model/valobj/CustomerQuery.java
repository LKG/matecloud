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
package vip.mate.crm.customer.domain.model.valobj;

import vip.mate.crm.common.CrmScope;

/**
 * 客户分页查询条件(含公海/私海维度 + 数据权限)。
 *
 * @param keyword    名称/电话模糊词
 * @param industry   行业过滤
 * @param level      等级过滤 A/B/C
 * @param status     合作状态过滤
 * @param poolStatus 1公海 0私海 null全部
 * @param ownerId    负责人过滤(「我的客户」传当前用户)
 * @param scope      数据权限范围(公海查询通常传 all)
 * @param pageNum    页码(1起)
 * @param pageSize   每页条数
 */
public record CustomerQuery(String keyword, String industry, String level, String status,
                            Integer poolStatus, String ownerId, CrmScope scope,
                            int pageNum, int pageSize) {
}
