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
package vip.mate.starter.sso.core.sync;

/**
 * Outcome of one organization sync run (also persisted to
 * {@code mate_identity_sync_log}).
 *
 * @param provider provider code
 * @param deptCount   departments processed
 * @param userAdded   members newly provisioned
 * @param userUpdated members updated
 * @param userRemoved members pruned (disabled/unlinked)
 * @param success     whether the run succeeded
 * @param error       error message when {@code success == false}
 *
 * @author mateaix
 */
public record SyncResult(
        String provider,
        int deptCount,
        int userAdded,
        int userUpdated,
        int userRemoved,
        boolean success,
        String error) {

    public static SyncResult ok(String provider, int deptCount, int added, int updated, int removed) {
        return new SyncResult(provider, deptCount, added, updated, removed, true, null);
    }

    public static SyncResult fail(String provider, String error) {
        return new SyncResult(provider, 0, 0, 0, 0, false, error);
    }
}
