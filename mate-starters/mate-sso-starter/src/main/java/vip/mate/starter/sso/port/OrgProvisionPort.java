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
package vip.mate.starter.sso.port;

import java.util.List;
import vip.mate.starter.sso.spi.model.ExternalDept;
import vip.mate.starter.sso.spi.model.ExternalUser;

/**
 * Outbound port (hexagonal): the starter delegates "write the org into <b>your</b>
 * domain tables" to the consuming service. The starter owns identity mapping; it
 * never touches business {@code user} / {@code dept} tables.
 *
 * <ul>
 *   <li>mate-system implements this writing to {@code mate_dept} / {@code mate_admin};</li>
 *   <li>mate-lms implements this writing to {@code mate_lms_member} / its dept tables.</li>
 * </ul>
 *
 * <p>Local ids are {@code String} (MateCloud VARCHAR(32) id convention). There is
 * intentionally <b>no default bean</b>: a service that enables organization sync
 * must supply one, otherwise sync has nowhere to land.
 *
 * @author mateaix
 */
public interface OrgProvisionPort {

    /**
     * Create or update a department, returning its local id.
     *
     * @param dept            external department
     * @param parentLocalId   local id of the parent ({@code null} for a root)
     * @param existingLocalId previously-bound local id, or {@code null} on first sync
     *                        (lets the impl update-in-place instead of duplicating)
     */
    String upsertDept(ExternalDept dept, String parentLocalId, String existingLocalId, ProviderRef ref);

    /**
     * Create or update a member, returning its local id.
     *
     * @param user            external user
     * @param localDeptIds    resolved local department ids
     * @param existingLocalId previously-bound local id, or {@code null} on first sync
     */
    String provisionUser(ExternalUser user, List<String> localDeptIds, String existingLocalId, ProviderRef ref);

    /** Mark a local user disabled (left the org), per {@code LeaveStrategy.DISABLE}. */
    void disableUser(String localUserId, ProviderRef ref);

    /** Unlink a local user from the org, per {@code LeaveStrategy.UNLINK}. */
    void unlinkUser(String localUserId, ProviderRef ref);

    /** Remove a local department that no longer exists upstream (FULL-sync prune). */
    void removeDept(String localDeptId, ProviderRef ref);
}
