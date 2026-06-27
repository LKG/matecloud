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
package vip.mate.system.domain.event;

import lombok.Getter;

@Getter
public class UserCreatedEvent extends BaseDomainEvent {

    private final String username;
    private final String mobile;

    public UserCreatedEvent(String aggregateId, String username, String mobile) {
        super(aggregateId, "UserAggregate");
        this.username = username;
        this.mobile = mobile;
    }

    @Override
    public String getEventType() { return "USER_CREATED"; }

    @Override
    public String getDescription() {
        return String.format("User created: %s (%s)", username, mobile);
    }
}
