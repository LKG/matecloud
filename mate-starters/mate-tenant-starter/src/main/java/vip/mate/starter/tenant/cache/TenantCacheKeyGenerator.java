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
package vip.mate.starter.tenant.cache;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.lang.NonNull;
import vip.mate.starter.tenant.core.TenantContext;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Spring Cache KeyGenerator that prefixes all cache keys with the current tenant ID.
 *
 * @author mateaix
 */
public class TenantCacheKeyGenerator implements KeyGenerator {

    @Override
    @NonNull
    public Object generate(@NonNull Object target, @NonNull Method method, @NonNull Object... params) {
        String tenantId = TenantContext.getTenantId();
        String base = target.getClass().getSimpleName() + ":" + method.getName()
                + ":" + Arrays.deepHashCode(params);
        if (tenantId == null || tenantId.isBlank()) {
            return base;
        }
        return "t:" + tenantId + ":" + base;
    }
}
