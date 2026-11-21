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
package vip.mate.crm.business.domain.model.valobj;

import vip.mate.crm.common.CrmScope;

/**
 * 商机分页查询条件(含数据权限)。
 *
 * @param keyword    名称/客户名模糊词
 * @param stage      阶段过滤
 * @param customerId 客户过滤
 * @param statusFlag 1进行中 0已结束 null全部
 * @param mineOnly   仅本人(当前用户ID)
 * @param scope      数据权限范围
 */
public record BusinessQuery(String keyword, String stage, String customerId, Integer statusFlag,
                            String ownerId, CrmScope scope, int pageNum, int pageSize) {
}
