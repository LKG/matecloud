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
package vip.mate.crm.product.application.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vip.mate.base.result.PageResult;
import vip.mate.crm.product.domain.adapter.repository.IProductRepository;
import vip.mate.crm.product.domain.model.aggregate.ProductAggregate;
import vip.mate.crm.product.domain.model.entity.Product;
import vip.mate.crm.query.dto.AdvancedQuery;

import java.util.List;

/** 产品读侧查询服务。 */
@Service
@RequiredArgsConstructor
public class ProductQueryService {

    private final IProductRepository productRepository;

    public PageResult<Product> page(String keyword, String category, Integer status, int pageNum, int pageSize) {
        PageResult<ProductAggregate> result = productRepository.page(keyword, category, status, pageNum, pageSize);
        List<Product> list = result.getList().stream().map(ProductAggregate::getProduct).toList();
        return PageResult.of(list, result.getTotal());
    }

    public Product findById(String id) {
        ProductAggregate agg = productRepository.findById(id);
        return agg == null ? null : agg.getProduct();
    }

    public PageResult<Product> search(AdvancedQuery query) {
        PageResult<ProductAggregate> result = productRepository.search(query);
        List<Product> list = result.getList().stream().map(ProductAggregate::getProduct).toList();
        return PageResult.of(list, result.getTotal());
    }

    public List<Product> listOnSale() {
        return productRepository.listOnSale().stream().map(ProductAggregate::getProduct).toList();
    }
}
