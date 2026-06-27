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
package vip.mate.starter.tenant.rpc;

import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import vip.mate.starter.tenant.core.MultiTenantType;
import vip.mate.starter.tenant.core.TenantContext;
import vip.mate.starter.tenant.core.TenantProperties;
import vip.mate.starter.tenant.core.TenantRuntime;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regression tests proving {@link TenantDubboProviderFilter#invoke} leaves no
 * tenant context on a pooled provider thread, even when downstream throws.
 *
 * @author mateaix
 */
class TenantDubboProviderFilterInvokeTest {

    private final TenantDubboProviderFilter filter = new TenantDubboProviderFilter();

    @AfterEach
    void reset() {
        TenantContext.clear();
        TenantRuntime.set(null);
    }

    private TenantProperties datasourceProps() {
        TenantProperties p = new TenantProperties();
        p.setType(MultiTenantType.DATASOURCE);
        p.setDefaultDsName("master");
        p.setDsPrefix("tenant_");
        p.setSuperTenantId("");
        return p;
    }

    @Test
    void clearsTenantContextWhenDownstreamThrows() {
        TenantRuntime.set(datasourceProps());

        Invocation inv = mock(Invocation.class);
        when(inv.getAttachment("tenantId")).thenReturn("1001");
        Invoker<?> invoker = mock(Invoker.class);
        when(invoker.invoke(inv)).thenThrow(new RpcException("boom"));

        assertThrows(RpcException.class, () -> filter.invoke(invoker, inv));

        // finally must have run: tenant context cleared, datasource stack balanced.
        assertNull(TenantContext.getTenantId());
        assertNull(com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder.peek());
    }

    @Test
    void clearsTenantContextWhenNoTenantBusinessRpcRejected() {
        TenantRuntime.set(datasourceProps());

        Invocation inv = mock(Invocation.class);
        when(inv.getAttachment("tenantId")).thenReturn(null);
        when(inv.getMethodName()).thenReturn("businessQuery");
        lenient().when(inv.getParameterTypes()).thenReturn(new Class<?>[]{String.class});
        lenient().when(inv.getArguments()).thenReturn(new Object[]{"id-1"});
        Invoker invoker = mock(Invoker.class);
        when(invoker.getInterface()).thenReturn(Runnable.class);

        // datasource mode + no tenant + non-cross-tenant method → RpcException, and cleanup runs.
        assertThrows(RpcException.class, () -> filter.invoke(invoker, inv));
        assertNull(TenantContext.getTenantId());
    }

    @Test
    void rejectsMalformedAttachmentBeforeTouchingContext() {
        TenantRuntime.set(datasourceProps());

        Invocation inv = mock(Invocation.class);
        when(inv.getAttachment("tenantId")).thenReturn("1' OR '1'='1");
        Invoker<?> invoker = mock(Invoker.class);

        assertThrows(RpcException.class, () -> filter.invoke(invoker, inv));
        assertNull(TenantContext.getTenantId());
    }
}
