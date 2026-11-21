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
package vip.mate.crm.business.domain.model.entity;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商机产品行(报价明细, money = 各行 subtotal 合计)。
 *
 * @author mateaix
 */
@Data
@Builder
public class BusinessProduct {

    private String id;
    private String businessId;
    private String productId;
    private String productName;
    private BigDecimal unitPrice;
    private BigDecimal quantity;
    private BigDecimal subtotal;

    public static BusinessProduct of(String productId, String productName,
                                     BigDecimal unitPrice, BigDecimal quantity) {
        BigDecimal qty = quantity == null ? BigDecimal.ONE : quantity;
        BigDecimal price = unitPrice == null ? BigDecimal.ZERO : unitPrice;
        return BusinessProduct.builder()
                .productId(productId)
                .productName(productName)
                .unitPrice(price)
                .quantity(qty)
                .subtotal(price.multiply(qty))
                .build();
    }
}
