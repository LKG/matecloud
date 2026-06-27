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

import org.junit.jupiter.api.Test;
import vip.mate.base.tenant.CrossTenantRpc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantDubboProviderFilterTest {

    interface SampleRpc {
        @CrossTenantRpc
        String loginLookup(String username);

        String businessQuery(String id);

        // Overloads: only the single-arg variant is cross-tenant.
        @CrossTenantRpc
        String find(String username);

        String find(String username, int tenantId);
    }

    private static final Class<?> IFACE = SampleRpc.class;

    private static boolean direct(String method, Class<?>... paramTypes) {
        return TenantDubboProviderFilter.isCrossTenantMethod(IFACE, method, paramTypes, null);
    }

    private static boolean generic(String genericMethod, Object[] args) {
        return TenantDubboProviderFilter.isCrossTenantMethod(IFACE, genericMethod, null, args);
    }

    @Test
    void annotatedDirectMethodAllowed() {
        assertTrue(direct("loginLookup", String.class));
    }

    @Test
    void plainDirectMethodNotCrossTenant() {
        assertFalse(direct("businessQuery", String.class));
    }

    @Test
    void unknownMethodFailsClosed() {
        assertFalse(direct("noSuchMethod", String.class));
        assertFalse(TenantDubboProviderFilter.isCrossTenantMethod(IFACE, null, null, null));
    }

    // ---- P2: exact-signature overload matching ----

    @Test
    void annotatedOverloadDoesNotWhitelistUnannotatedOverload() {
        assertTrue(direct("find", String.class));                 // annotated overload
        assertFalse(direct("find", String.class, int.class));     // unannotated overload
    }

    // ---- generic invocation ----

    @Test
    void genericInvokeOfAnnotatedMethodAllowed() {
        assertTrue(generic("$invoke", new Object[]{
                "loginLookup", new String[]{"java.lang.String"}, new Object[]{"alice"}}));
    }

    @Test
    void genericInvokeOfBusinessMethodFailsClosed() {
        assertFalse(generic("$invoke", new Object[]{
                "businessQuery", new String[]{"java.lang.String"}, new Object[]{"id-1"}}));
    }

    @Test
    void genericInvokeResolvesOverloadBySignature() {
        assertTrue(generic("$invoke", new Object[]{
                "find", new String[]{"java.lang.String"}, new Object[]{"alice"}}));
        assertFalse(generic("$invoke", new Object[]{
                "find", new String[]{"java.lang.String", "int"}, new Object[]{"alice", 1}}));
    }

    @Test
    void genericInvokeWithUnresolvableTypeFailsClosed() {
        assertFalse(generic("$invoke", new Object[]{
                "loginLookup", new String[]{"com.does.not.Exist"}, new Object[]{"x"}}));
    }

    @Test
    void genericInvokeWithoutTargetFailsClosed() {
        assertFalse(generic("$invoke", new Object[]{}));
        assertFalse(generic("$invokeAsync", null));
    }

    // ---- P3: only $echo allowed ----

    @Test
    void echoAllowedOtherPseudoMethodsFailClosed() {
        assertTrue(direct("$echo", String.class));
        assertFalse(direct("$somethingElse", String.class));
    }
}
