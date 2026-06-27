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
package vip.mate.ai.application.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.mate.ai.domain.adapter.repository.IProviderRepository;
import vip.mate.ai.domain.model.aggregate.ProviderAggregate;
import vip.mate.ai.infrastructure.support.ApiKeyCipher;
import vip.mate.ai.types.exception.AiErrorCode;
import vip.mate.base.exception.BizException;

import java.time.LocalDateTime;

/**
 * Provider config CRUD. Note: the {@code apiKey} field on the incoming
 * payload is the PLAINTEXT key — the service encrypts it before persisting.
 * The {@code apiKeyCipher} field on {@link ProviderAggregate} stores the
 * encrypted form and is what flows in and out of the repository.
 *
 * @author mateaix
 */
@Service
@RequiredArgsConstructor
public class ProviderCommandService {

    private final IProviderRepository providerRepository;
    private final ApiKeyCipher apiKeyCipher;

    /**
     * Create a provider. {@code plainApiKey} is REQUIRED on create.
     */
    @Transactional(rollbackFor = Exception.class)
    public String create(ProviderAggregate input, String plainApiKey) {
        if (plainApiKey == null || plainApiKey.isBlank()) {
            throw new BizException(AiErrorCode.PROVIDER_API_KEY_REQUIRED);
        }
        if (providerRepository.findByCode(input.getCode()) != null) {
            throw new BizException(AiErrorCode.DUPLICATE_PROVIDER_CODE);
        }
        ProviderAggregate fresh = ProviderAggregate.newOne(input.getCode(), input.getName(), input.getVendor());
        fresh.applyUpdate(input);
        fresh.assignApiKey(apiKeyCipher.encrypt(plainApiKey));
        providerRepository.save(fresh);
        if (fresh.isDefaultProvider()) {
            providerRepository.clearDefaultExcept(fresh.getId());
        }
        return fresh.getId();
    }

    /**
     * Update a provider. {@code plainApiKey} is OPTIONAL — when blank, the
     * existing encrypted key is kept untouched.
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(String id, ProviderAggregate input, String plainApiKey) {
        ProviderAggregate existing = providerRepository.findById(id);
        if (existing == null) {
            throw new BizException(AiErrorCode.PROVIDER_NOT_EXIST);
        }
        existing.applyUpdate(input);
        if (plainApiKey != null && !plainApiKey.isBlank()) {
            existing.assignApiKey(apiKeyCipher.encrypt(plainApiKey));
        }
        providerRepository.update(existing);
        if (existing.isDefaultProvider()) {
            providerRepository.clearDefaultExcept(existing.getId());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        ProviderAggregate existing = providerRepository.findById(id);
        if (existing == null) {
            throw new BizException(AiErrorCode.PROVIDER_NOT_EXIST);
        }
        providerRepository.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void toggleEnabled(String id, boolean enabled) {
        ProviderAggregate existing = providerRepository.findById(id);
        if (existing == null) {
            throw new BizException(AiErrorCode.PROVIDER_NOT_EXIST);
        }
        if (enabled) {
            existing.enable();
        } else {
            existing.disable();
        }
        providerRepository.update(existing);
    }

    /**
     * Test a provider's API key by attempting to decrypt and ping the
     * vendor endpoint. MVP: we only verify decryption works and record
     * the timestamp. A real ping would be vendor-specific.
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean test(String id) {
        ProviderAggregate existing = providerRepository.findById(id);
        if (existing == null) {
            throw new BizException(AiErrorCode.PROVIDER_NOT_EXIST);
        }
        boolean ok;
        try {
            String plain = apiKeyCipher.decrypt(existing.getApiKeyCipher());
            ok = plain != null && !plain.isBlank();
        } catch (Exception e) {
            ok = false;
        }
        existing.markTested(ok, LocalDateTime.now());
        providerRepository.update(existing);
        return ok;
    }
}
