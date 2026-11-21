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
package vip.mate.crm.query.domain.adapter.repository;

import vip.mate.crm.query.domain.model.QueryField;

import java.util.List;

/**
 * 可查字段元数据仓储接口(domain 定义, infrastructure 实现)。
 *
 * @author mateaix
 */
public interface IQueryFieldRepository {

    /** 某模块全部可查字段(按 sort 升序)。 */
    List<QueryField> listByModule(String moduleKey);

    /** 按 (module, fieldKey) 取字段元数据, 不存在返回 null(白名单拒绝依据)。 */
    QueryField find(String moduleKey, String fieldKey);
}
