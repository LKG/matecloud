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
package vip.mate.crm.product.infrastructure.adapter.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vip.mate.base.result.PageResult;
import vip.mate.crm.product.domain.adapter.repository.IProductRepository;
import vip.mate.crm.product.domain.model.aggregate.ProductAggregate;
import vip.mate.crm.product.domain.model.entity.Product;
import vip.mate.crm.product.infrastructure.dao.ProductDao;
import vip.mate.crm.product.infrastructure.dao.po.ProductPO;

import java.util.List;

/** 产品仓储实现。 */
@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements IProductRepository {

    private final ProductDao productDao;

    @Override
    public void save(ProductAggregate aggregate) {
        productDao.insert(toPO(aggregate.getProduct()));
    }

    @Override
    public void update(ProductAggregate aggregate) {
        productDao.updateById(toPO(aggregate.getProduct()));
    }

    @Override
    public ProductAggregate findById(String id) {
        ProductPO po = productDao.selectById(id);
        return po == null ? null : ProductAggregate.of(toEntity(po));
    }

    @Override
    public void deleteById(String id) {
        productDao.deleteById(id);
    }

    @Override
    public PageResult<ProductAggregate> page(String keyword, String category, Integer status,
                                             int pageNum, int pageSize) {
        LambdaQueryWrapper<ProductPO> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(ProductPO::getProductName, kw)
                    .or().like(ProductPO::getProductCode, kw));
        }
        wrapper.eq(category != null && !category.isBlank(), ProductPO::getCategory, category)
                .eq(status != null, ProductPO::getStatus, status)
                .orderByAsc(ProductPO::getSort).orderByDesc(ProductPO::getCreatedAt);

        Page<ProductPO> result = productDao.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<ProductAggregate> list = result.getRecords().stream()
                .map(po -> ProductAggregate.of(toEntity(po)))
                .toList();
        return PageResult.of(list, result.getTotal());
    }

    @Override
    public List<ProductAggregate> listOnSale() {
        LambdaQueryWrapper<ProductPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductPO::getStatus, ProductAggregate.ON).orderByAsc(ProductPO::getSort);
        return productDao.selectList(wrapper).stream().map(po -> ProductAggregate.of(toEntity(po))).toList();
    }

    private ProductPO toPO(Product p) {
        ProductPO po = new ProductPO();
        po.setId(p.getId());
        po.setProductName(p.getProductName());
        po.setProductCode(p.getProductCode());
        po.setCategory(p.getCategory());
        po.setPrice(p.getPrice());
        po.setUnit(p.getUnit());
        po.setStatus(p.getStatus());
        po.setDescription(p.getDescription());
        po.setSort(p.getSort());
        po.setCreatorId(p.getCreatorId());
        return po;
    }

    private Product toEntity(ProductPO po) {
        return Product.builder()
                .id(po.getId())
                .productName(po.getProductName())
                .productCode(po.getProductCode())
                .category(po.getCategory())
                .price(po.getPrice())
                .unit(po.getUnit())
                .status(po.getStatus())
                .description(po.getDescription())
                .sort(po.getSort())
                .creatorId(po.getCreatorId())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
}
