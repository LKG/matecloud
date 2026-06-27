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
package vip.mate.starter.sso.provider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import vip.mate.base.channel.ProviderDescriptor;
import vip.mate.base.exception.BizException;
import vip.mate.starter.sso.core.AccessTokenCache;
import vip.mate.starter.sso.core.http.HttpExecutor;
import vip.mate.starter.sso.spi.AbstractOAuthIdentityProvider;
import vip.mate.starter.sso.spi.annotation.IdentityProvider;
import vip.mate.starter.sso.spi.model.ExternalDept;
import vip.mate.starter.sso.spi.model.ExternalUser;
import vip.mate.starter.sso.spi.model.ProviderConfig;
import vip.mate.starter.sso.types.SsoErrorCode;

/**
 * 飞书(Feishu / Lark)provider — OAuth login + contacts sync over the open-apis
 * REST endpoints. The cached access token is the <em>tenant_access_token</em>;
 * contacts calls authenticate via the {@code Authorization: Bearer} header.
 *
 * <p>Login: {@code code → user_access_token} (oidc) → {@code user_id}
 * ({@code /authen/v1/user_info}) → user detail. Departments come from
 * {@code departments/0/children?fetch_child=true} in one paged sweep; users are
 * paged per department via {@code users/find_by_department}.
 *
 * <p>Config keys: {@code appId}, {@code appSecret}.
 *
 * @author mateaix
 */
@IdentityProvider(value = "feishu", describe = "飞书")
public class FeishuProvider extends AbstractOAuthIdentityProvider {

    private static final String BASE = "https://open.feishu.cn/open-apis";

    public FeishuProvider(HttpExecutor http, AccessTokenCache tokenCache) {
        super(http, tokenCache);
    }

    @Override
    public String code() {
        return "feishu";
    }

    @Override
    public ProviderDescriptor descriptor() {
        return ProviderDescriptor.builder("feishu", "飞书")
                .describe("飞书扫码登录 + 通讯录同步")
                .secret("appId", "应用 AppId", true)
                .secret("appSecret", "应用 AppSecret", true)
                .build();
    }

    // ---- OAuth template steps ----

    @Override
    protected AccessTokenCache.Token requestAccessToken(ProviderConfig cfg) {
        Map<String, Object> r = http.postJson(BASE + "/auth/v3/tenant_access_token/internal",
                Map.of("app_id", cfg.require("appId"), "app_secret", cfg.require("appSecret")));
        checkCode(r, "tenant_access_token");
        return new AccessTokenCache.Token(str(r.get("tenant_access_token")), asLong(r.get("expire"), 7200));
    }

    @Override
    protected String exchangeCodeForUserId(String code, String accessToken, ProviderConfig cfg) {
        // code → user_access_token
        Map<String, Object> tk = http.postJson(BASE + "/authen/v1/oidc/access_token",
                Map.of("grant_type", "authorization_code", "code", code), bearer(accessToken));
        checkCode(tk, "user_access_token");
        String userAccessToken = str(asMap(tk.get("data")).get("access_token"));
        // user_access_token → user_id
        Map<String, Object> info = http.getJson(BASE + "/authen/v1/user_info", bearer(userAccessToken));
        checkCode(info, "user_id");
        return str(asMap(info.get("data")).get("user_id"));
    }

    @Override
    protected ExternalUser fetchUserDetail(String externalId, String accessToken, ProviderConfig cfg) {
        Map<String, Object> r = http.getJson(
                BASE + "/contact/v3/users/" + externalId + "?user_id_type=user_id&department_id_type=department_id",
                bearer(accessToken));
        checkCode(r, "user_info");
        return toUser(asMap(asMap(r.get("data")).get("user")));
    }

    // ---- organization sync ----

    @Override
    @SuppressWarnings("unchecked")
    public List<ExternalDept> fetchDepartments(ProviderConfig cfg) {
        String token = accessToken(cfg);
        List<ExternalDept> out = new ArrayList<>();
        String pageToken = null;
        do {
            String url = BASE + "/contact/v3/departments/0/children"
                    + "?department_id_type=department_id&fetch_child=true&page_size=50"
                    + (pageToken == null ? "" : "&page_token=" + pageToken);
            Map<String, Object> r = http.getJson(url, bearer(token));
            checkCode(r, "department");
            Map<String, Object> data = asMap(r.get("data"));
            for (Map<String, Object> d : (List<Map<String, Object>>) data.getOrDefault("items", List.of())) {
                out.add(new ExternalDept(
                        str(d.get("department_id")),
                        str(d.get("name")),
                        str(d.get("parent_department_id")),
                        (int) asLong(d.get("order"), 0),
                        d));
            }
            pageToken = Boolean.TRUE.equals(data.get("has_more")) ? str(data.get("page_token")) : null;
        } while (pageToken != null && !pageToken.isBlank());
        return out;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ExternalUser> fetchUsers(ProviderConfig cfg, List<ExternalDept> departments) {
        String token = accessToken(cfg);
        List<ExternalUser> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ExternalDept dept : departments) {
            String pageToken = null;
            do {
                String url = BASE + "/contact/v3/users/find_by_department"
                        + "?user_id_type=user_id&department_id_type=department_id&page_size=50"
                        + "&department_id=" + dept.externalId()
                        + (pageToken == null ? "" : "&page_token=" + pageToken);
                Map<String, Object> r = http.getJson(url, bearer(token));
                checkCode(r, "user_list");
                Map<String, Object> data = asMap(r.get("data"));
                for (Map<String, Object> u : (List<Map<String, Object>>) data.getOrDefault("items", List.of())) {
                    ExternalUser user = toUser(u);
                    if (user.externalId() != null && seen.add(user.externalId())) {
                        out.add(user);
                    }
                }
                pageToken = Boolean.TRUE.equals(data.get("has_more")) ? str(data.get("page_token")) : null;
            } while (pageToken != null && !pageToken.isBlank());
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private ExternalUser toUser(Map<String, Object> u) {
        List<String> deptIds = new ArrayList<>();
        Object dept = u.get("department_ids");
        if (dept instanceof List<?> l) {
            for (Object o : l) {
                deptIds.add(str(o));
            }
        }
        Map<String, Object> status = asMap(u.get("status"));
        boolean enabled = !Boolean.FALSE.equals(status.get("is_activated"))
                && !Boolean.TRUE.equals(status.get("is_frozen"))
                && !Boolean.TRUE.equals(status.get("is_resigned"));
        return new ExternalUser(
                str(u.get("user_id")),
                str(u.get("union_id")),
                str(u.get("name")),
                str(u.get("mobile")),
                str(u.get("email")),
                str(asMap(u.get("avatar")).get("avatar_240")),
                deptIds,
                enabled,
                u);
    }

    // ---- helpers ----

    private static Map<String, String> bearer(String token) {
        return Map.of("Authorization", "Bearer " + token);
    }

    private void checkCode(Map<String, Object> r, String what) {
        long code = asLong(r.get("code"), 0);
        if (code != 0) {
            throw new BizException(SsoErrorCode.SSO_E_HTTP.getCode(),
                    "飞书" + what + "获取失败: " + r.get("msg"));
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : Map.of();
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static long asLong(Object o, long def) {
        if (o instanceof Number n) {
            return n.longValue();
        }
        try {
            return o == null ? def : Long.parseLong(String.valueOf(o));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
