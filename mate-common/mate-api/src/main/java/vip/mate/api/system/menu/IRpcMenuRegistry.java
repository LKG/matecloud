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
package vip.mate.api.system.menu;

import vip.mate.base.result.Result;
import vip.mate.base.tenant.CrossTenantRpc;

import java.util.List;

/**
 * Dubbo RPC interface for startup-time menu/permission self-registration.
 *
 * <p>Implemented by mate-system, consumed by any business module that ships a
 * {@code menu-manifest.yml}. Menus are idempotently upserted keyed by the stable
 * string {@code code}; numeric ids are allocated internally by mate-system.</p>
 *
 * <p>{@code mate_menu} is a global table and registration happens before any
 * tenant context exists, hence {@link CrossTenantRpc}.</p>
 *
 * @author mateaix
 */
public interface IRpcMenuRegistry {

    /**
     * Idempotently upsert the given menu tree under the owning module.
     *
     * @param moduleCode owning module code (from {@code mate.module.code})
     * @param menus      menu tree (children nested); flattened on the server
     * @return number of nodes processed (inserted or updated)
     */
    @CrossTenantRpc
    Result<Integer> register(String moduleCode, List<MenuNode> menus);
}
