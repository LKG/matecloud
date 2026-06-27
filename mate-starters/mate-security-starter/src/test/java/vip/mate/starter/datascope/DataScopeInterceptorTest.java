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
package vip.mate.starter.datascope;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DataScopeInterceptorTest {

    private final DataScopeInterceptor interceptor = new DataScopeInterceptor();
    private static final String BASE = "SELECT * FROM mate_user WHERE 1 = 1";

    private DataScopeContext.DataScopeContextBuilder base(DataScope scope) {
        return DataScopeContext.builder()
                .scope(scope)
                .deptAlias("dept_id")
                .userAlias("creator_id");
    }

    @Test
    void deptScopeAppendsQuotedEquality() {
        String sql = interceptor.buildScopeSql(BASE, base(DataScope.DEPT).deptId("100").build());
        assertEquals(BASE + " AND dept_id = '100'", sql);
    }

    @Test
    void deptScopeAcceptsUuidHexId() {
        String sql = interceptor.buildScopeSql(BASE,
                base(DataScope.DEPT).deptId("a1b2c3d4e5f6").build());
        assertEquals(BASE + " AND dept_id = 'a1b2c3d4e5f6'", sql);
    }

    @Test
    void selfScopeAppendsQuotedEquality() {
        String sql = interceptor.buildScopeSql(BASE, base(DataScope.SELF).userId("42").build());
        assertEquals(BASE + " AND creator_id = '42'", sql);
    }

    @Test
    void deptAndChildBuildsSubquery() {
        String sql = interceptor.buildScopeSql(BASE, base(DataScope.DEPT_AND_CHILD).deptId("7").build());
        assertEquals(BASE + " AND dept_id IN (SELECT id FROM mate_dept WHERE id = '7'"
                + " OR ancestors LIKE CONCAT('7', ',%'))", sql);
    }

    @Test
    void customScopeRecombinesValidatedIds() {
        String sql = interceptor.buildScopeSql(BASE, base(DataScope.CUSTOM).customDeptIds("1, 2 ,3").build());
        assertEquals(BASE + " AND dept_id IN ('1', '2', '3')", sql);
    }

    @Test
    void injectionInDeptIdFailsClosed() {
        String sql = interceptor.buildScopeSql(BASE, base(DataScope.DEPT).deptId("1 OR 1=1").build());
        assertEquals(BASE + " AND 1 = 0", sql);
    }

    @Test
    void injectionInCustomListFailsClosed() {
        String sql = interceptor.buildScopeSql(BASE,
                base(DataScope.CUSTOM).customDeptIds("1,2); DROP TABLE mate_user;--").build());
        assertEquals(BASE + " AND 1 = 0", sql);
    }

    @Test
    void blankCustomListFailsClosed() {
        String sql = interceptor.buildScopeSql(BASE, base(DataScope.CUSTOM).customDeptIds("  ").build());
        assertEquals(BASE + " AND 1 = 0", sql);
    }

    @Test
    void nullDeptIdFailsClosed() {
        String sql = interceptor.buildScopeSql(BASE, base(DataScope.DEPT).deptId(null).build());
        assertEquals(BASE + " AND 1 = 0", sql);
    }

    @Test
    void allScopeIsUntouched() {
        String sql = interceptor.buildScopeSql(BASE, base(DataScope.ALL).build());
        assertEquals(BASE, sql);
    }
}
