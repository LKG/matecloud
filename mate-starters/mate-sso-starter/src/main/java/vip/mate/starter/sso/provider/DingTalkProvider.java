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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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
 * 钉钉(DingTalk)provider — OAuth scan login + contacts sync over the legacy
 * {@code oapi.dingtalk.com} REST API (no Aliyun Tea SDK pulled in).
 *
 * <p>Login is three-hop: scan {@code tmp_auth_code → unionId} (sns, signed with
 * the app secret) → {@code userid} (getbyunionid) → user detail. Sync walks
 * {@code department/listsub} top-down and pages {@code user/list} per department.
 * The {@code scanAppId} field is for the front-end DDLogin widget.
 *
 * <p>Config keys: {@code appKey}, {@code appSecret} (token + sns signature),
 * {@code corpId} (optional), {@code scanAppId} (optional, front-end only).
 *
 * @author mateaix
 */
@IdentityProvider(value = "dingtalk", describe = "钉钉")
public class DingTalkProvider extends AbstractOAuthIdentityProvider {

    private static final String BASE = "https://oapi.dingtalk.com";
    /** Root department id in DingTalk. */
    private static final long ROOT_DEPT = 1L;

    public DingTalkProvider(HttpExecutor http, AccessTokenCache tokenCache) {
        super(http, tokenCache);
    }

    @Override
    public String code() {
        return "dingtalk";
    }

    @Override
    public ProviderDescriptor descriptor() {
        return ProviderDescriptor.builder("dingtalk", "钉钉")
                .describe("钉钉扫码登录 + 通讯录同步")
                .text("corpId", "企业 CorpId", false)
                .secret("appKey", "应用 AppKey", true)
                .secret("appSecret", "应用 AppSecret", true)
                .text("scanAppId", "扫码登录 AppId", false)
                .build();
    }

    // ---- OAuth template steps ----

    @Override
    protected AccessTokenCache.Token requestAccessToken(ProviderConfig cfg) {
        Map<String, Object> r = http.getJson(BASE + "/gettoken?appkey=" + cfg.require("appKey")
                + "&appsecret=" + cfg.require("appSecret"));
        checkErrcode(r, "access_token");
        return new AccessTokenCache.Token(str(r.get("access_token")), asLong(r.get("expires_in"), 7200));
    }

    @Override
    protected String exchangeCodeForUserId(String code, String accessToken, ProviderConfig cfg) {
        String unionId = unionIdByCode(code, cfg);
        // unionId → userid (getbyunionid)
        Map<String, Object> r = http.postJson(BASE + "/topapi/user/getbyunionid?access_token=" + accessToken,
                Map.of("unionid", unionId));
        checkErrcode(r, "userid");
        Map<String, Object> result = asMap(r.get("result"));
        return str(result.get("userid"));
    }

    @Override
    protected ExternalUser fetchUserDetail(String externalId, String accessToken, ProviderConfig cfg) {
        Map<String, Object> r = http.postJson(BASE + "/topapi/v2/user/get?access_token=" + accessToken,
                Map.of("userid", externalId, "language", "zh_CN"));
        checkErrcode(r, "user_info");
        return toUser(asMap(r.get("result")));
    }

    /** sns: tmp_auth_code → unionId, signed with HMAC-SHA256(timestamp, appSecret). */
    private String unionIdByCode(String code, ProviderConfig cfg) {
        String appKey = cfg.require("appKey");
        String appSecret = cfg.require("appSecret");
        String timestamp = String.valueOf(System.currentTimeMillis());
        String signature = urlEncode(hmacBase64(timestamp, appSecret));
        String url = BASE + "/sns/getuserinfo_bycode?signature=" + signature
                + "&timestamp=" + timestamp + "&accessKey=" + appKey;
        Map<String, Object> r = http.postJson(url, Map.of("tmp_auth_code", code));
        checkErrcode(r, "unionId");
        return str(asMap(r.get("user_info")).get("unionid"));
    }

    // ---- organization sync ----

    @Override
    public List<ExternalDept> fetchDepartments(ProviderConfig cfg) {
        String token = accessToken(cfg);
        List<ExternalDept> out = new ArrayList<>();
        collectDepts(ROOT_DEPT, token, out);
        return out;
    }

    @SuppressWarnings("unchecked")
    private void collectDepts(long parentId, String token, List<ExternalDept> out) {
        Map<String, Object> r = http.postJson(BASE + "/topapi/v2/department/listsub?access_token=" + token,
                Map.of("dept_id", parentId));
        checkErrcode(r, "department");
        List<Map<String, Object>> list = (List<Map<String, Object>>) r.getOrDefault("result", List.of());
        for (Map<String, Object> d : list) {
            long deptId = asLong(d.get("dept_id"), 0);
            out.add(new ExternalDept(
                    String.valueOf(deptId),
                    str(d.get("name")),
                    str(d.getOrDefault("parent_id", parentId)),
                    (int) asLong(d.get("order"), 0),
                    d));
            collectDepts(deptId, token, out); // recurse into children
        }
    }

    @Override
    public List<ExternalUser> fetchUsers(ProviderConfig cfg, List<ExternalDept> departments) {
        String token = accessToken(cfg);
        List<ExternalUser> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ExternalDept dept : departments) {
            long cursor = 0;
            while (true) {
                cursor = pageUsers(dept.externalId(), cursor, token, out, seen);
                if (cursor < 0) {
                    break;
                }
            }
        }
        return out;
    }

    /** One page of a department's users; returns next cursor, or -1 when exhausted. */
    @SuppressWarnings("unchecked")
    private long pageUsers(String deptId, long cursor, String token, List<ExternalUser> out, Set<String> seen) {
        Map<String, Object> r = http.postJson(BASE + "/topapi/v2/user/list?access_token=" + token,
                Map.of("dept_id", Long.parseLong(deptId), "cursor", cursor, "size", 50));
        checkErrcode(r, "user_list");
        Map<String, Object> result = asMap(r.get("result"));
        List<Map<String, Object>> list = (List<Map<String, Object>>) result.getOrDefault("list", List.of());
        for (Map<String, Object> u : list) {
            ExternalUser user = toUser(u);
            if (user.externalId() != null && seen.add(user.externalId())) {
                out.add(user);
            }
        }
        boolean hasMore = Boolean.TRUE.equals(result.get("has_more"));
        return hasMore ? asLong(result.get("next_cursor"), -1) : -1;
    }

    @SuppressWarnings("unchecked")
    private ExternalUser toUser(Map<String, Object> u) {
        List<String> deptIds = new ArrayList<>();
        Object dept = u.get("dept_id_list");
        if (dept instanceof List<?> l) {
            for (Object o : l) {
                deptIds.add(str(o));
            }
        }
        boolean enabled = !Boolean.FALSE.equals(u.get("active")); // active=false → not yet activated
        return new ExternalUser(
                str(u.get("userid")),
                str(u.get("unionid")),
                str(u.get("name")),
                str(u.get("mobile")),
                str(u.get("email")),
                str(u.get("avatar")),
                deptIds,
                enabled,
                u);
    }

    // ---- helpers ----

    private void checkErrcode(Map<String, Object> r, String what) {
        long code = asLong(r.get("errcode"), 0);
        if (code != 0) {
            throw new BizException(SsoErrorCode.SSO_E_HTTP.getCode(),
                    "钉钉" + what + "获取失败: " + r.get("errmsg"));
        }
    }

    private static String hmacBase64(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] sig = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(sig);
        } catch (Exception e) {
            throw new BizException(SsoErrorCode.SSO_E_HTTP.getCode(), "钉钉签名计算失败: " + e.getMessage());
        }
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
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
