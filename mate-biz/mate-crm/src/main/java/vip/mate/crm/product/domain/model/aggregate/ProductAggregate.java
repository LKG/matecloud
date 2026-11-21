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
package vip.mate.crm.product.domain.model.aggregate;

import lombok.Builder;
import lombok.Getter;
import vip.mate.base.exception.BizException;
import vip.mate.base.response.ResponseCode;
import vip.mate.crm.product.domain.model.entity.Product;
import vip.mate.crm.types.exception.CrmErrorCode;
import vip.mate.starter.distribute.util.SnowflakeUtil;

import java.math.BigDecimal;

/**
 * 产品聚合根: 上下架切换 + 价格非负不变式。
 *
 * @author mateaix
 */
@Getter
@Builder
public class ProductAggregate {

    public static final int ON = 1;
    public static final int OFF = 0;

    private final Product product;

    public static ProductAggregate create(String productName, String productCode, String category,
                                          BigDecimal price, String unit, String description,
                                          Integer sort, String creatorId) {
        requireText(productName, "产品名称不能为空");
        if (price != null && price.signum() < 0) {
            throw new BizException(ResponseCode.PARAM_VALID_ERROR.getCode(), "价格不能为负");
        }
        Product product = Product.builder()
                .productName(productName)
                .productCode(productCode)
                .category(category)
                .price(price == null ? BigDecimal.ZERO : price)
                .unit(unit)
                .status(ON)
                .description(description)
                .sort(sort == null ? 0 : sort)
                .creatorId(creatorId)
                .build();
        product.setId(SnowflakeUtil.newSnowflakeId());
        return ProductAggregate.builder().product(product).build();
    }

    public static ProductAggregate of(Product product) {
        if (product == null) {
            throw new BizException(CrmErrorCode.RECORD_NOT_FOUND);
        }
        return ProductAggregate.builder().product(product).build();
    }

    public void updateInfo(String productName, String productCode, String category, BigDecimal price,
                           String unit, String description, Integer sort) {
        requireText(productName, "产品名称不能为空");
        if (price != null && price.signum() < 0) {
            throw new BizException(ResponseCode.PARAM_VALID_ERROR.getCode(), "价格不能为负");
        }
        product.setProductName(productName);
        product.setProductCode(productCode);
        product.setCategory(category);
        if (price != null) {
            product.setPrice(price);
        }
        product.setUnit(unit);
        product.setDescription(description);
        if (sort != null) {
            product.setSort(sort);
        }
    }

    public void switchStatus(int status) {
        product.setStatus(status);
    }

    public String getId() {
        return product.getId();
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BizException(ResponseCode.PARAM_VALID_ERROR.getCode(), message);
        }
    }
}
