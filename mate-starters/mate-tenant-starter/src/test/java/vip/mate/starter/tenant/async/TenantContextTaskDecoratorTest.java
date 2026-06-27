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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import vip.mate.starter.tenant.core.TenantContext;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TenantContextTaskDecoratorTest {

    private final TenantContextTaskDecorator decorator = new TenantContextTaskDecorator();

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void rebindsCapturedTenantOnWorkerThread() throws InterruptedException {
        TenantContext.setTenantId("acme");
        AtomicReference<String> seen = new AtomicReference<>("<unset>");

        // Decorate on THIS thread (capturing "acme"), then run on another thread
        // that has no tenant of its own.
        Runnable decorated = decorator.decorate(() -> seen.set(TenantContext.getTenantId()));
        Thread worker = new Thread(decorated);
        worker.start();
        worker.join();

        assertEquals("acme", seen.get(), "captured tenant must be rebound on the worker thread");
    }

    @Test
    void restoresAndClearsWorkerThreadAfterRun() {
        TenantContext.setTenantId("acme");
        Runnable decorated = decorator.decorate(() -> { /* no-op */ });

        // Simulate a pooled worker that already carried a different tenant.
        TenantContext.setTenantId("worker-prev");
        decorated.run();
        assertEquals("worker-prev", TenantContext.getTenantId(),
                "worker's previous tenant must be restored after the task");
    }

    @Test
    void clearsWorkerThreadWhenItHadNoPreviousTenant() {
        TenantContext.setTenantId("acme");
        Runnable decorated = decorator.decorate(() -> { /* no-op */ });

        // Worker thread starts with no tenant; after the task it must be clean
        // again (pooled threads are reused — no tenant bleed-over).
        TenantContext.clear();
        decorated.run();
        assertNull(TenantContext.getTenantId(),
                "worker thread must be cleared when it had no tenant before the task");
    }

    @Test
    void capturesNoTenantWhenSubmitterHadNone() {
        TenantContext.clear();
        AtomicReference<String> seen = new AtomicReference<>("<unset>");
        Runnable decorated = decorator.decorate(() -> seen.set(TenantContext.getTenantId()));

        // Worker happens to carry a stale tenant; the decorator must blank it
        // because the submitter had none.
        TenantContext.setTenantId("stale");
        decorated.run();
        assertNull(seen.get(), "no captured tenant must blank the worker thread during the task");
    }
}
