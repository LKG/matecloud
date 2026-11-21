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
package vip.mate.crm.product.application.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.mate.base.exception.BizException;
import vip.mate.crm.common.CurrentUserResolver;
import vip.mate.crm.product.domain.adapter.repository.IProductRepository;
import vip.mate.crm.product.domain.model.aggregate.ProductAggregate;
import vip.mate.crm.types.exception.CrmErrorCode;

/**
 * 产品写侧应用服务。
 *
 * @author mateaix
 */
@Service
@RequiredArgsConstructor
public class ProductCommandService {

    private final IProductRepository productRepository;
    private final CurrentUserResolver currentUserResolver;

    @Transactional
    public String create(ProductCommand cmd) {
        ProductAggregate agg = ProductAggregate.create(
                cmd.getProductName(), cmd.getProductCode(), cmd.getCategory(), cmd.getPrice(),
                cmd.getUnit(), cmd.getDescription(), cmd.getSort(), currentUserResolver.require().userId());
        productRepository.save(agg);
        return agg.getId();
    }

    @Transactional
    public void update(String id, ProductCommand cmd) {
        ProductAggregate agg = findOrThrow(id);
        agg.updateInfo(cmd.getProductName(), cmd.getProductCode(), cmd.getCategory(), cmd.getPrice(),
                cmd.getUnit(), cmd.getDescription(), cmd.getSort());
        productRepository.update(agg);
    }

    @Transactional
    public void switchStatus(String id, int status) {
        ProductAggregate agg = findOrThrow(id);
        agg.switchStatus(status);
        productRepository.update(agg);
    }

    @Transactional
    public void delete(String id) {
        findOrThrow(id);
        productRepository.deleteById(id);
    }

    private ProductAggregate findOrThrow(String id) {
        ProductAggregate agg = productRepository.findById(id);
        if (agg == null) {
            throw new BizException(CrmErrorCode.RECORD_NOT_FOUND);
        }
        return agg;
    }
}
