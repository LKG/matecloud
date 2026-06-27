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
package vip.mate.ai.application.query.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vip.mate.ai.application.query.IProviderQueryService;
import vip.mate.ai.domain.adapter.repository.IProviderRepository;
import vip.mate.ai.domain.model.aggregate.ProviderAggregate;
import vip.mate.ai.infrastructure.support.ApiKeyCipher;
import vip.mate.ai.types.exception.AiErrorCode;
import vip.mate.base.exception.BizException;
import vip.mate.base.result.PageResult;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProviderQueryServiceImpl implements IProviderQueryService {

    private final IProviderRepository providerRepository;
    private final ApiKeyCipher apiKeyCipher;

    @Override
    public PageResult<ProviderView> page(int pageNum, int pageSize, String keyword,
                                         String vendor, Integer enabled) {
        PageResult<ProviderAggregate> raw = providerRepository.page(
                pageNum, pageSize, keyword, vendor, enabled);
        List<ProviderView> list = raw.getList().stream().map(this::toView).toList();
        return PageResult.of(list, raw.getTotal());
    }

    @Override
    public List<ProviderView> listEnabled() {
        return providerRepository.listEnabled().stream().map(this::toView).toList();
    }

    @Override
    public ProviderView detail(String id) {
        ProviderAggregate p = providerRepository.findById(id);
        if (p == null) {
            throw new BizException(AiErrorCode.PROVIDER_NOT_EXIST);
        }
        return toView(p);
    }

    private ProviderView toView(ProviderAggregate p) {
        return new ProviderView(
                p.getId(), p.getCode(), p.getName(), p.getVendor(), p.getBaseUrl(),
                apiKeyCipher.preview(p.getApiKeyCipher()),
                p.getDefaultModel(), p.getAvailableModels(),
                p.getTemperature(), p.getMaxTokens(),
                p.getEnabled(), p.getIsDefault(),
                p.getLastTestAt(), p.getLastTestOk(), p.getSort());
    }
}
