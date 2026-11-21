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
package vip.mate.crm.product.domain.adapter.repository;

import vip.mate.base.result.PageResult;
import vip.mate.crm.product.domain.model.aggregate.ProductAggregate;

import java.util.List;

/**
 * 产品仓储接口(共享目录, 无数据权限维度)。
 *
 * @author mateaix
 */
public interface IProductRepository {

    void save(ProductAggregate aggregate);

    void update(ProductAggregate aggregate);

    ProductAggregate findById(String id);

    void deleteById(String id);

    PageResult<ProductAggregate> page(String keyword, String category, Integer status, int pageNum, int pageSize);

    /** 上架产品简表(商机/合同选品用)。 */
    List<ProductAggregate> listOnSale();
}
