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
package vip.mate.crm.product.domain.model.entity;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 产品/价格表实体(共享目录, 无负责人维度)。
 *
 * @author mateaix
 */
@Data
@Builder
public class Product {

    private String id;
    private String productName;
    private String productCode;
    private String category;
    private BigDecimal price;
    private String unit;
    /** 1=上架 0=下架 */
    private Integer status;
    private String description;
    private Integer sort;
    private String creatorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
