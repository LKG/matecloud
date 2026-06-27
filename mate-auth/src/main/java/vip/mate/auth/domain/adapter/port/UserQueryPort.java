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
package vip.mate.auth.domain.adapter.port;

import vip.mate.auth.domain.model.aggregate.AuthUser;
import vip.mate.auth.domain.model.valobj.Account;

/**
 * Outbound port: look up a user by account (username / mobile / email).
 * <p>
 * The infrastructure implementation talks to mate-system via Dubbo RPC.
 * Domain and application layers depend only on this interface.
 *
 * @author mateaix
 */
public interface UserQueryPort {

    /**
     * Look up a user by any kind of account. Returns {@code null} if no user
     * matches — the caller is responsible for throwing the right
     * {@code BizException}.
     */
    AuthUser findByAccount(Account account);

    /**
     * Look up by user id.
     */
    AuthUser findById(String userId);
}
