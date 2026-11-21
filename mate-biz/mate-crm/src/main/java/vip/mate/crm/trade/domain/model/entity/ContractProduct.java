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
package vip.mate.crm.trade.domain.model.entity;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 合同签单产品行(subtotal = unitPrice * quantity * discount, 折后金额)。
 *
 * @author mateaix
 */
@Data
@Builder
public class ContractProduct {

    private String id;
    private String contractId;
    private String productId;
    private String productName;
    private BigDecimal unitPrice;
    private BigDecimal quantity;
    /** 折扣率(0-1), 默认 1 */
    private BigDecimal discount;
    private BigDecimal subtotal;

    public static ContractProduct of(String productId, String productName,
                                     BigDecimal unitPrice, BigDecimal quantity, BigDecimal discount) {
        BigDecimal price = unitPrice == null ? BigDecimal.ZERO : unitPrice;
        BigDecimal qty = quantity == null ? BigDecimal.ONE : quantity;
        BigDecimal disc = discount == null ? BigDecimal.ONE : discount;
        BigDecimal sub = price.multiply(qty).multiply(disc).setScale(2, RoundingMode.HALF_UP);
        return ContractProduct.builder()
                .productId(productId)
                .productName(productName)
                .unitPrice(price)
                .quantity(qty)
                .discount(disc)
                .subtotal(sub)
                .build();
    }
}
