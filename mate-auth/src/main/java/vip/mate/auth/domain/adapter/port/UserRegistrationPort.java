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

import vip.mate.api.system.command.RegisterUserCommand;
import vip.mate.auth.domain.model.aggregate.AuthUser;

/**
 * Outbound port: create a user in the write model (mate-system).
 *
 * @author mateaix
 */
public interface UserRegistrationPort {

    AuthUser register(RegisterUserCommand command);
}
