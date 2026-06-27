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

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import vip.mate.base.tenant.CrossTenantRpc;
import vip.mate.starter.tenant.core.TenantContext;
import vip.mate.starter.tenant.core.TenantId;
import vip.mate.starter.tenant.core.TenantRuntime;
import vip.mate.starter.tenant.datasource.TenantDataSourceHelper;
import vip.mate.starter.tenant.datasource.TenantDataSourceWebFilter;

import java.util.Map;

/**
 * Dubbo provider filter that restores the tenant id from the RPC attachment and,
 * in SCHEMA / DATASOURCE mode, routes the call to the tenant's datasource — the
 * RPC-side counterpart of {@link TenantDataSourceWebFilter}. Without this, a
 * provider query in datasource mode would fall back to the primary/master
 * datasource instead of {@code tenant_<id>}.
 *
 * @author mateaix
 */
@Slf4j
@Activate(group = "provider", order = -1000)
public class TenantDubboProviderFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String tenantId = invocation.getAttachment("tenantId");
        // Reject a present-but-malformed attachment before touching any thread
        // state, so there is nothing to clean up on this path.
        if (tenantId != null && !tenantId.isBlank() && !TenantId.isValid(tenantId)) {
            throw new RpcException("Invalid tenant attachment");
        }

        // From here on we may mutate thread-locals (TenantContext / datasource
        // stack); everything runs inside try/finally so a pooled provider thread
        // is always left clean — even if routing setup below throws.
        boolean dsPushed = false;
        try {
            if (tenantId != null && !tenantId.isBlank()) {
                TenantContext.setTenantId(tenantId);
            }
            if (TenantRuntime.isDatasourceMode()) {
                String current = TenantContext.getTenantId();
                if (current != null && !current.isBlank()) {
                    TenantDataSourceHelper.push(
                            TenantDataSourceHelper.resolveDsName(TenantRuntime.get(), current));
                    dsPushed = true;
                } else if (!isCrossTenant(invoker, invocation)) {
                    // SCHEMA/DATASOURCE mode + no tenant + not an explicit
                    // cross-tenant (auth/identity) method → fail closed instead of
                    // silently running on the primary/master datasource.
                    throw new RpcException("Missing tenant for datasource-routed RPC: "
                            + invoker.getInterface().getName() + "#" + invocation.getMethodName());
                }
            }
            return invoker.invoke(invocation);
        } finally {
            if (dsPushed) {
                TenantDataSourceHelper.poll();
            }
            TenantContext.clear();
        }
    }

    private static final Map<String, Class<?>> PRIMITIVES = Map.of(
            "int", int.class, "long", long.class, "boolean", boolean.class,
            "double", double.class, "float", float.class, "short", short.class,
            "byte", byte.class, "char", char.class, "void", void.class);

    private static boolean isCrossTenant(Invoker<?> invoker, Invocation invocation) {
        return isCrossTenantMethod(invoker.getInterface(), invocation.getMethodName(),
                invocation.getParameterTypes(), invocation.getArguments());
    }

    /**
     * True if the target RPC method is marked {@link CrossTenantRpc} (allowed to
     * run without a tenant). Fails closed (false) for anything that cannot be
     * resolved to a concrete annotated method.
     *
     * <p>Resolution is by exact signature, not name alone, so an annotated
     * overload cannot whitelist an unannotated same-named overload:
     * <ul>
     *   <li>Direct calls use {@code parameterTypes} for an exact {@code getMethod}.</li>
     *   <li>Generic {@code $invoke}/{@code $invokeAsync} unwrap the real method
     *       name (arg 0) and parameter-type names (arg 1, {@code String[]}) and
     *       rebuild the signature; unresolvable types fail closed.</li>
     *   <li>Only {@code $echo} among other framework pseudo-methods is allowed;
     *       any other {@code $*} fails closed.</li>
     * </ul>
     * Package-private for testing.
     */
    static boolean isCrossTenantMethod(Class<?> iface, String methodName,
                                       Class<?>[] parameterTypes, Object[] arguments) {
        if (iface == null || methodName == null) {
            return false;
        }
        if ("$invoke".equals(methodName) || "$invokeAsync".equals(methodName)) {
            if (arguments == null || arguments.length < 2
                    || !(arguments[0] instanceof String realName)) {
                return false;
            }
            Class<?>[] realTypes = resolveTypeNames(arguments[1], iface.getClassLoader());
            if (realTypes == null) {
                return false;
            }
            return isAnnotated(iface, realName, realTypes);
        }
        if (methodName.startsWith("$")) {
            // Only the no-data-access echo pseudo-method is allowed; everything
            // else ($*) fails closed so future Dubbo internals can't widen the gap.
            return "$echo".equals(methodName);
        }
        return isAnnotated(iface, methodName, parameterTypes == null ? new Class<?>[0] : parameterTypes);
    }

    private static boolean isAnnotated(Class<?> iface, String name, Class<?>[] types) {
        try {
            return iface.getMethod(name, types).isAnnotationPresent(CrossTenantRpc.class);
        } catch (NoSuchMethodException e) {
            log.warn("Cannot resolve RPC method {}#{} for tenant check; failing closed",
                    iface.getName(), name);
            return false;
        }
    }

    /** Resolve generic invocation parameter-type names ({@code String[]}); null = unresolvable. */
    private static Class<?>[] resolveTypeNames(Object typeNames, ClassLoader cl) {
        if (typeNames == null) {
            return new Class<?>[0];
        }
        if (!(typeNames instanceof String[] names)) {
            return null;
        }
        Class<?>[] types = new Class<?>[names.length];
        for (int i = 0; i < names.length; i++) {
            Class<?> primitive = PRIMITIVES.get(names[i]);
            if (primitive != null) {
                types[i] = primitive;
                continue;
            }
            try {
                types[i] = Class.forName(names[i], false, cl);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }
        return types;
    }
}
