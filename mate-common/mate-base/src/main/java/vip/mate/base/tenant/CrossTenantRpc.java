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
package vip.mate.base.tenant;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an RPC method that legitimately runs WITHOUT a tenant context — i.e. a
 * cross-tenant identity / authentication lookup that happens before a tenant is
 * established (resolve user by username, fetch roles for login, …).
 *
 * <p>In SCHEMA / DATASOURCE isolation mode the tenant Dubbo provider filter
 * fails closed: an RPC arriving with no tenant attachment is rejected unless its
 * method carries this annotation, in which case it is allowed to run on the
 * primary/master datasource. Tenant-scoped business RPCs must therefore NOT be
 * annotated, so a missing tenant attachment surfaces as an error instead of a
 * silent cross-tenant / master read.
 *
 * <p>Has no effect in COLUMN mode (there the row-level interceptor already fails
 * closed at query time) or when multi-tenancy is disabled.
 *
 * @author mateaix
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CrossTenantRpc {
}
