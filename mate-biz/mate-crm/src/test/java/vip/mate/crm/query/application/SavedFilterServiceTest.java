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
package vip.mate.crm.query.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vip.mate.base.exception.BizException;
import vip.mate.crm.common.CrmCurrentUser;
import vip.mate.crm.common.CurrentUserResolver;
import vip.mate.crm.query.domain.adapter.repository.ISavedFilterRepository;
import vip.mate.crm.query.domain.model.SavedFilter;
import vip.mate.crm.query.dto.AdvancedQuery;
import vip.mate.crm.query.dto.ConditionGroup;
import vip.mate.crm.query.dto.QueryCondition;
import vip.mate.crm.query.dto.SavedFilterRequest;
import vip.mate.crm.query.dto.SavedFilterVO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SavedFilterService personal-isolation tests")
class SavedFilterServiceTest {

    /** 内存版仓储, 记录 listByUser 收到的 userId 以便验证隔离。 */
    private static class InMemoryRepo implements ISavedFilterRepository {
        final Map<String, SavedFilter> store = new HashMap<>();
        final List<String> listedUserIds = new ArrayList<>();

        @Override
        public List<SavedFilter> listByUser(String userId, String moduleKey) {
            listedUserIds.add(userId);
            return store.values().stream()
                    .filter(f -> f.getUserId().equals(userId)
                            && (moduleKey == null || moduleKey.equals(f.getModuleKey())))
                    .toList();
        }

        @Override
        public SavedFilter findById(String id) {
            return store.get(id);
        }

        @Override
        public void save(SavedFilter filter) {
            store.put(filter.getId(), filter);
        }

        @Override
        public void update(SavedFilter filter) {
            store.put(filter.getId(), filter);
        }

        @Override
        public void deleteById(String id) {
            store.remove(id);
        }
    }

    private static SavedFilterService service(InMemoryRepo repo, String currentUserId) {
        CurrentUserResolver resolver = new CurrentUserResolver() {
            @Override
            public CrmCurrentUser require() {
                return new CrmCurrentUser(currentUserId, currentUserId, null, "d1");
            }
        };
        return new SavedFilterService(repo, resolver, new ObjectMapper());
    }

    private static SavedFilterRequest request(String module, String name) {
        SavedFilterRequest req = new SavedFilterRequest();
        req.setModuleKey(module);
        req.setName(name);
        AdvancedQuery q = new AdvancedQuery();
        QueryCondition c = new QueryCondition();
        c.setFieldKey("leadName");
        c.setOperator("LIKE");
        c.setValues(List.of("张"));
        ConditionGroup g = new ConditionGroup();
        g.setConditions(List.of(c));
        q.setGroups(List.of(g));
        req.setQuery(q);
        return req;
    }

    @Test
    @DisplayName("save() forces ownership to current user and round-trips query json")
    void save_owns_and_serializes() {
        InMemoryRepo repo = new InMemoryRepo();
        String id = service(repo, "u1").save(request("lead", "我的展会线索"));

        SavedFilter stored = repo.findById(id);
        assertNotNull(stored);
        assertEquals("u1", stored.getUserId());
        assertEquals("我的展会线索", stored.getName());

        SavedFilterVO vo = service(repo, "u1").list("lead").get(0);
        assertEquals(id, vo.getId());
        assertEquals("LIKE", vo.getQuery().getGroups().get(0).getConditions().get(0).getOperator());
    }

    @Test
    @DisplayName("list() queries by current user id only (tenant isolation of filters)")
    void list_isolated_by_user() {
        InMemoryRepo repo = new InMemoryRepo();
        service(repo, "u1").save(request("lead", "u1 的筛选"));

        List<SavedFilterVO> asU2 = service(repo, "u2").list("lead");
        assertTrue(asU2.isEmpty());
        assertTrue(repo.listedUserIds.contains("u2"), "repository must be asked with caller's own userId");

        List<SavedFilterVO> asU1 = service(repo, "u1").list("lead");
        assertEquals(1, asU1.size());
    }

    @Test
    @DisplayName("delete()/update() of another user's filter rejected with CRMB002")
    void cross_user_mutation_denied() {
        InMemoryRepo repo = new InMemoryRepo();
        String id = service(repo, "u1").save(request("lead", "u1 的筛选"));

        SavedFilterService u2 = service(repo, "u2");
        SavedFilterRequest steal = request("lead", "改名覆盖");
        steal.setId(id);
        BizException e = assertThrows(BizException.class, () -> u2.save(steal));
        assertEquals("CRMB002", e.getCode());

        BizException e2 = assertThrows(BizException.class, () -> u2.delete(id));
        assertEquals("CRMB002", e2.getCode());
        assertNotNull(repo.findById(id), "record must survive the denied delete");
    }

    @Test
    @DisplayName("update() with own id overwrites name and query")
    void update_own_ok() {
        InMemoryRepo repo = new InMemoryRepo();
        SavedFilterService svc = service(repo, "u1");
        String id = svc.save(request("lead", "旧名"));

        SavedFilterRequest upd = request("lead", "新名");
        upd.setId(id);
        svc.save(upd);

        SavedFilterVO vo = svc.list("lead").get(0);
        assertEquals("新名", vo.getName());
        assertEquals(id, vo.getId());
        assertEquals(1, repo.store.size());
    }
}
