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

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

/**
 * AOP aspect that populates DataScopeHolder before annotated query methods.
 *
 * @author mateaix
 */
@Slf4j
@Aspect
public class DataScopeAspect {

    @Before("@annotation(dp)")
    public void before(JoinPoint jp, DataPermission dp) {
        try {
            Object loginId = StpUtil.getLoginIdDefaultNull();
            if (loginId == null) return;
            String userId = loginId.toString();
            Object scopeObj = StpUtil.getSession().get("dataScope");
            Object deptIdObj = StpUtil.getSession().get("deptId");
            Object customDeptIdsObj = StpUtil.getSession().get("customDeptIds");

            int scopeCode = scopeObj != null ? Integer.parseInt(scopeObj.toString()) : 1;
            DataScope scope = DataScope.of(scopeCode);

            if (scope == DataScope.ALL) {
                return;
            }

            DataScopeContext ctx = DataScopeContext.builder()
                    .scope(scope)
                    .deptAlias(dp.deptAlias())
                    .userAlias(dp.userAlias())
                    .userId(userId)
                    .deptId(deptIdObj != null ? deptIdObj.toString() : null)
                    .customDeptIds(customDeptIdsObj != null ? customDeptIdsObj.toString() : null)
                    .includeTables(dp.includeTables())
                    .build();
            DataScopeHolder.set(ctx);
        } catch (Exception e) {
            log.warn("DataScope aspect failed to resolve context: {}", e.getMessage());
        }
    }

    @After("@annotation(dp)")
    public void after(JoinPoint jp, DataPermission dp) {
        DataScopeHolder.clear();
    }
}
