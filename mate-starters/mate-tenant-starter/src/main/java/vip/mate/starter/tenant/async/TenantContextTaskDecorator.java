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
package vip.mate.starter.tenant.async;

import org.springframework.core.task.TaskDecorator;
import vip.mate.starter.tenant.core.TenantContext;

/**
 * Propagates the current {@link TenantContext} across thread-pool boundaries.
 *
 * <p>{@link TenantContext} is backed by a plain {@code ThreadLocal}, so an
 * {@code @Async} method (or any {@code TaskExecutor}-dispatched task) would
 * otherwise run on a worker thread with NO tenant — and the fail-closed
 * row-level interceptor would then reject every whitelisted-table query. This
 * decorator captures the tenant on the <em>submitting</em> thread and re-binds
 * it on the worker thread for the duration of the task, restoring the worker's
 * previous value afterwards (pooled threads are reused, so cleanup is
 * mandatory to avoid tenant bleed-over between tasks).
 *
 * @author mateaix
 */
public class TenantContextTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // Captured on the submitting thread, at submit time.
        String captured = TenantContext.getTenantId();
        return () -> {
            String previous = TenantContext.getTenantId();
            try {
                if (captured != null) {
                    TenantContext.setTenantId(captured);
                } else {
                    TenantContext.clear();
                }
                runnable.run();
            } finally {
                if (previous == null) {
                    TenantContext.clear();
                } else {
                    TenantContext.setTenantId(previous);
                }
            }
        };
    }
}
