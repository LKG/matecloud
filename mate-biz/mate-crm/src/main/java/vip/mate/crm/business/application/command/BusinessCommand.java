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
package vip.mate.crm.business.application.command;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 商机新增/编辑命令(含产品行)。
 *
 * @author mateaix
 */
@Data
public class BusinessCommand {

    private String businessName;
    private String customerId;
    private String customerName;
    private String contactName;
    private String source;
    private Integer dealProb;
    private LocalDate dealDate;
    private String nextStep;
    private String remark;
    private List<ProductLine> products;

    /** 报价产品行。 */
    @Data
    public static class ProductLine {
        private String productId;
        private String productName;
        private BigDecimal unitPrice;
        private BigDecimal quantity;
    }
}
