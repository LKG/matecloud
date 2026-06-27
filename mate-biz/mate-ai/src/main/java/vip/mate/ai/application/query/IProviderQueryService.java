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
package vip.mate.ai.application.query;

import vip.mate.base.result.PageResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface IProviderQueryService {

    PageResult<ProviderView> page(int pageNum, int pageSize, String keyword, String vendor, Integer enabled);

    List<ProviderView> listEnabled();

    ProviderView detail(String id);

    /**
     * Read model. {@code apiKeyMasked} is the only thing we ever expose for
     * the encrypted key — the real key never crosses the API boundary again
     * after the original create / update.
     */
    record ProviderView(String id, String code, String name, String vendor, String baseUrl,
                        String apiKeyMasked, String defaultModel, String availableModels,
                        BigDecimal temperature, Integer maxTokens,
                        Integer enabled, Integer isDefault,
                        LocalDateTime lastTestAt, Integer lastTestOk, Integer sort) {}
}
