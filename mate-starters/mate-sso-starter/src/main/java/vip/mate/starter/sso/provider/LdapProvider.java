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

import vip.mate.base.channel.ProviderDescriptor;
import vip.mate.base.exception.BizException;
import vip.mate.starter.sso.spi.AuthKind;
import vip.mate.starter.sso.spi.annotation.IdentityProvider;
import vip.mate.starter.sso.spi.model.AuthRequest;
import vip.mate.starter.sso.spi.model.ExternalDept;
import vip.mate.starter.sso.spi.model.ExternalUser;
import vip.mate.starter.sso.spi.model.ProviderConfig;
import vip.mate.starter.sso.types.SsoErrorCode;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * LDAP / Active Directory provider — directory bind (no OAuth). Borrows PlayEdu's
 * {@code LdapUtil} approach: admin-bind to locate the user DN by uid/mail, then
 * re-bind as that DN with the supplied password to verify.
 *
 * <p>First cut implements {@link #authenticate}; full contacts sync
 * (OU-chain → dept tree, paged search, {@code userAccountControl} disable
 * detection) is a clearly-marked follow-up.
 *
 * <p>Config keys: {@code url}, {@code baseDN}, {@code adminDN}, {@code adminPass},
 * {@code filterScope}, {@code excludeScope}.
 *
 * @author mateaix
 */
@IdentityProvider(value = "ldap", describe = "LDAP / AD")
public class LdapProvider implements vip.mate.starter.sso.spi.IdentityProvider {

    @Override
    public String code() {
        return "ldap";
    }

    @Override
    public AuthKind authKind() {
        return AuthKind.DIRECTORY_BIND;
    }

    @Override
    public ProviderDescriptor descriptor() {
        return ProviderDescriptor.builder("ldap", "LDAP / AD")
                .describe("OpenLDAP / Windows AD 直连校验 + 通讯录同步")
                .text("url", "服务器地址", true, "ldaps://ad.corp.com:636")
                .text("baseDN", "Base DN", true, "dc=corp,dc=com")
                .text("adminDN", "管理员 DN", true)
                .secret("adminPass", "管理员密码", true)
                .text("filterScope", "查询范围 filter_scope", false)
                .text("excludeScope", "排除范围 exclude_scope", false)
                .build();
    }

    @Override
    public ExternalUser authenticate(AuthRequest request, ProviderConfig cfg) {
        String account = request.username();
        boolean isMail = account != null && account.contains("@");
        String filter = (isMail ? "(mail=" : "(uid=") + account + ")";

        // 1) admin bind to locate the user entry
        InitialDirContext admin = bind(cfg.require("url"), cfg.require("adminDN"), cfg.require("adminPass"));
        try {
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            sc.setReturningAttributes(new String[]{"entryUUID", "uid", "cn", "displayName", "mail", "mobile"});
            NamingEnumeration<SearchResult> results = admin.search(cfg.require("baseDN"), filter, sc);
            if (!results.hasMore()) {
                throw new BizException(SsoErrorCode.SSO_B_NOT_BOUND.getCode(), "LDAP 用户不存在: " + account);
            }
            SearchResult entry = results.next();
            String userDn = entry.getNameInNamespace();

            // 2) re-bind as the user to verify the password
            bind(cfg.require("url"), userDn, request.password()).close();

            return toUser(entry.getAttributes());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(SsoErrorCode.SSO_E_LDAP.getCode(), "LDAP 校验失败: " + e.getMessage());
        } finally {
            close(admin);
        }
    }

    @Override
    public List<ExternalDept> fetchDepartments(ProviderConfig cfg) {
        // TODO: paged OU search (PagedResultsControl), build dept tree from OU chain.
        throw new UnsupportedOperationException("LdapProvider.fetchDepartments: implement OU-chain sync (follow-up)");
    }

    @Override
    public List<ExternalUser> fetchUsers(ProviderConfig cfg, List<ExternalDept> departments) {
        // TODO: paged user search + userAccountControl disable detection.
        throw new UnsupportedOperationException("LdapProvider.fetchUsers: implement paged user sync (follow-up)");
    }

    private InitialDirContext bind(String url, String principal, String credentials) {
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.PROVIDER_URL, url);
            env.put(Context.SECURITY_PRINCIPAL, principal);
            env.put(Context.SECURITY_CREDENTIALS, credentials);
            return new InitialDirContext(env);
        } catch (Exception e) {
            throw new BizException(SsoErrorCode.SSO_E_LDAP.getCode(), "LDAP 绑定失败: " + e.getMessage());
        }
    }

    private ExternalUser toUser(Attributes a) throws Exception {
        return new ExternalUser(
                attr(a, "entryUUID"),
                null,
                firstNonNull(attr(a, "displayName"), attr(a, "cn")),
                attr(a, "mobile"),
                attr(a, "mail"),
                null,
                List.of(),
                true,
                Map.of());
    }

    private static String attr(Attributes a, String name) throws Exception {
        return a.get(name) == null ? null : String.valueOf(a.get(name).get());
    }

    private static String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }

    private static void close(InitialDirContext ctx) {
        try {
            if (ctx != null) {
                ctx.close();
            }
        } catch (Exception ignored) {
            // best-effort
        }
    }
}
