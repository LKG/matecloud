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
package vip.mate.system.admin.domain.permission.model.aggregate;

import lombok.Builder;
import lombok.Data;
import vip.mate.system.admin.domain.permission.model.entity.Role;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class RoleAggregate implements Serializable {
    private Role role;
    @Builder.Default
    private List<String> menuIds = new ArrayList<>();

    public String getId() { return role.getId(); }

    public void assignMenus(List<String> menuIds) {
        this.menuIds = new ArrayList<>(menuIds);
    }
}
