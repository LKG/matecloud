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
package vip.mate.crm.common;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import vip.mate.base.constant.AuthHeaders;
import vip.mate.base.exception.BizException;
import vip.mate.crm.types.exception.CrmErrorCode;

import java.util.Arrays;
import java.util.List;

/**
 * 当前用户与数据权限解析器。
 *
 * <p>优先从 Sa-Token 会话读取(userId/username/realName/deptId, 登录时由
 * mate-system 写入); 会话不可用(微服务模式下网关已鉴权)回退网关透传头
 * {@link AuthHeaders#USER_ID} 等。数据范围读取会话 dataScope/deptId/
 * customDeptIds(DataScopeSessionInterceptor 写入), 缺失按「全部」。
 *
 * @author mateaix
 */
@Slf4j
@Component
public class CurrentUserResolver {

    /** 当前登录用户, 未登录返回 null。 */
    public CrmCurrentUser getOrNull() {
        try {
            Object loginId = StpUtil.getLoginIdDefaultNull();
            if (loginId != null) {
                String userId = loginId.toString();
                String username = sessionString("username");
                String realName = sessionString("realName");
                String deptId = sessionString("deptId");
                return new CrmCurrentUser(userId, username, realName, deptId);
            }
        } catch (Exception e) {
            log.debug("Sa-Token session unavailable, falling back to gateway headers: {}", e.getMessage());
        }
        HttpServletRequest request = currentRequest();
        if (request != null) {
            String userId = request.getHeader(AuthHeaders.USER_ID);
            if (userId != null && !userId.isBlank()) {
                return new CrmCurrentUser(userId, request.getHeader(AuthHeaders.USER_NAME),
                        null, request.getHeader(AuthHeaders.DEPT_ID));
            }
        }
        return null;
    }

    /** 当前登录用户, 未登录抛业务异常。 */
    public CrmCurrentUser require() {
        CrmCurrentUser user = getOrNull();
        if (user == null) {
            throw new BizException(CrmErrorCode.NO_PERMISSION.getCode(), "未登录或登录已过期");
        }
        return user;
    }

    /** 当前用户的数据权限范围快照, 会话缺失按全部处理。 */
    public CrmScope scope() {
        CrmCurrentUser user = getOrNull();
        if (user == null) {
            return CrmScope.all();
        }
        try {
            int code = parseInt(sessionString("dataScope"), CrmScope.ALL);
            String deptId = sessionString("deptId");
            if (deptId == null || deptId.isBlank()) {
                deptId = user.deptId();
            }
            List<String> customDeptIds = List.of();
            String csv = sessionString("customDeptIds");
            if (csv != null && !csv.isBlank()) {
                customDeptIds = Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
            }
            return new CrmScope(code, user.userId(), deptId, customDeptIds);
        } catch (Exception e) {
            log.debug("Data scope session keys missing, defaulting to ALL: {}", e.getMessage());
            return CrmScope.all();
        }
    }

    private static String sessionString(String key) {
        Object value = StpUtil.getSession().get(key);
        return value == null ? null : value.toString();
    }

    private static int parseInt(String value, int fallback) {
        try {
            return value == null ? fallback : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }
}
