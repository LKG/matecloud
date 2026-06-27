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
package vip.mate.system.admin.trigger.rpc;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.util.StringUtils;
import vip.mate.api.rpc.RpcConstants;
import vip.mate.api.system.menu.IRpcMenuRegistry;
import vip.mate.api.system.menu.MenuNode;
import vip.mate.base.result.Result;
import vip.mate.starter.tenant.core.TenantHelper;
import vip.mate.system.admin.infrastructure.dao.MenuDao;
import vip.mate.system.admin.infrastructure.dao.RoleMenuDao;
import vip.mate.system.admin.infrastructure.dao.po.MenuPO;
import vip.mate.system.admin.infrastructure.dao.po.RoleMenuPO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Startup-time menu/permission self-registration endpoint (P1).
 *
 * <p>Business modules ship a {@code menu-manifest.yml} and call {@link #register}
 * at boot. Menus are idempotently upserted keyed by the stable string
 * {@code code}; the numeric {@code id} is allocated here (MyBatis Plus
 * {@code ASSIGN_ID}). On the first run for pre-existing menus the {@code code} is
 * {@code null} in the DB, so we <em>adopt</em> the matching legacy row (by path /
 * perms / name+type+parent) and backfill its {@code code}+{@code module_code} —
 * this is why re-running never produces duplicates.</p>
 *
 * <p>{@code mate_menu} / {@code mate_role_menu} are global tables and registration
 * occurs with no tenant context, so all writes run inside
 * {@link TenantHelper#withoutTenant}.</p>
 *
 * @author mateaix
 */
@Slf4j
@DubboService(version = RpcConstants.VERSION, group = RpcConstants.GROUP_SYSTEM)
@RequiredArgsConstructor
public class RpcMenuRegistryServiceImpl implements IRpcMenuRegistry {

    /** Super-admin role id — every registered menu is granted to it. */
    private static final String SUPER_ADMIN_ROLE_ID = "1";

    private final MenuDao menuDao;
    private final RoleMenuDao roleMenuDao;

    @Override
    public Result<Integer> register(String moduleCode, List<MenuNode> menus) {
        if (!StringUtils.hasText(moduleCode)) {
            return Result.fail("moduleCode is required for menu registration");
        }
        if (menus == null || menus.isEmpty()) {
            return Result.ok(0);
        }
        try {
            int processed = TenantHelper.withoutTenant(() -> doRegister(moduleCode, menus));
            log.info("[menu] module {} registered {} nodes", moduleCode, processed);
            return Result.ok(processed);
        } catch (Exception e) {
            log.error("[menu] module {} registration failed", moduleCode, e);
            return Result.fail("menu registration failed: " + e.getMessage());
        }
    }

    private int doRegister(String moduleCode, List<MenuNode> menus) {
        // Pre-order flatten (parents before children) so parentCode is resolvable.
        List<MenuNode> flat = new ArrayList<>();
        flatten(menus, flat);

        // code -> resolved menu id. Seed with rows already carrying a code so
        // parent lookups can hit previously-registered nodes.
        Map<String, String> codeToId = new HashMap<>();
        for (MenuPO existing : menuDao.selectList(
                new LambdaQueryWrapper<MenuPO>().isNotNull(MenuPO::getCode))) {
            if (StringUtils.hasText(existing.getCode())) {
                codeToId.put(existing.getCode(), existing.getId());
            }
        }

        int processed = 0;
        for (MenuNode node : flat) {
            if (!StringUtils.hasText(node.getCode())) {
                log.warn("[menu] module {} skip node without code: name={}", moduleCode, node.getName());
                continue;
            }
            String parentId = resolveParentId(node, codeToId);
            MenuPO match = findExisting(node, parentId);
            String id = (match != null)
                    ? updateExisting(match, node, parentId, moduleCode)
                    : insertNew(node, parentId, moduleCode);
            codeToId.put(node.getCode(), id);
            processed++;
        }
        reconcileModule(moduleCode, flat);
        pruneEmptyDirectories();
        return processed;
    }

    /**
     * 模块声明式对账: manifest 即该模块菜单的全量声明 —— 归属本 moduleCode 但本次未声明的 code
     * 一律删除 (改名/重组/拔 starter 后的孤儿不再残留)。手工菜单 (无 moduleCode) 不受影响。
     */
    private void reconcileModule(String moduleCode, List<MenuNode> declared) {
        Set<String> declaredCodes = new HashSet<>();
        for (MenuNode n : declared) {
            if (StringUtils.hasText(n.getCode())) {
                declaredCodes.add(n.getCode());
            }
        }
        if (declaredCodes.isEmpty()) {
            return;
        }
        List<MenuPO> owned = menuDao.selectList(new LambdaQueryWrapper<MenuPO>()
                .eq(MenuPO::getModuleCode, moduleCode).isNotNull(MenuPO::getCode));
        for (MenuPO po : owned) {
            if (!declaredCodes.contains(po.getCode())) {
                roleMenuDao.delete(new LambdaQueryWrapper<RoleMenuPO>()
                        .eq(RoleMenuPO::getMenuId, po.getId()));
                menuDao.deleteById(po.getId());
                log.info("[menu] module {} reconciled away stale node code={} name={}",
                        moduleCode, po.getCode(), po.getName());
            }
        }
    }

    /**
     * 清理空目录: 自注册的 M 型目录在 manifest 重组后可能不再有子节点 (如分组结构调整),
     * 空目录渲染无意义且误导。仅处理带 code 的行 (自注册产物), 手工菜单不受影响。
     * 自下而上最多三轮 (目录嵌套通常 ≤2 层); 幂等 — 组目录若仍被其它 manifest 声明,
     * 下次注册会重新 upsert。
     */
    private void pruneEmptyDirectories() {
        for (int round = 0; round < 3; round++) {
            List<MenuPO> dirs = menuDao.selectList(new LambdaQueryWrapper<MenuPO>()
                    .eq(MenuPO::getType, "M").isNotNull(MenuPO::getCode));
            int removed = 0;
            for (MenuPO dir : dirs) {
                Long children = menuDao.selectCount(
                        new LambdaQueryWrapper<MenuPO>().eq(MenuPO::getParentId, dir.getId()));
                if (children == null || children == 0) {
                    roleMenuDao.delete(new LambdaQueryWrapper<RoleMenuPO>()
                            .eq(RoleMenuPO::getMenuId, dir.getId()));
                    menuDao.deleteById(dir.getId());
                    log.info("[menu] pruned empty directory code={} name={}", dir.getCode(), dir.getName());
                    removed++;
                }
            }
            if (removed == 0) {
                return;
            }
        }
    }

    private void flatten(List<MenuNode> nodes, List<MenuNode> out) {
        if (nodes == null) {
            return;
        }
        for (MenuNode n : nodes) {
            out.add(n);
            flatten(n.getChildren(), out);
        }
    }

    private String resolveParentId(MenuNode node, Map<String, String> codeToId) {
        if (!StringUtils.hasText(node.getParentCode())) {
            return null;
        }
        String id = codeToId.get(node.getParentCode());
        if (id != null) {
            return id;
        }
        MenuPO parent = menuDao.selectOne(new LambdaQueryWrapper<MenuPO>()
                .eq(MenuPO::getCode, node.getParentCode()).last("LIMIT 1"));
        if (parent != null) {
            codeToId.put(node.getParentCode(), parent.getId());
            return parent.getId();
        }
        log.warn("[menu] parent code '{}' not found for node '{}'; inserting with null parent",
                node.getParentCode(), node.getCode());
        return null;
    }

    /**
     * Adopt chain — first match wins:
     * <ol>
     *   <li>by {@code code} (exact stable key);</li>
     *   <li>else, if path present, by {@code path};</li>
     *   <li>else, if perms present, by {@code perms};</li>
     *   <li>else (directory with neither) by name + type + same parent.</li>
     * </ol>
     */
    private MenuPO findExisting(MenuNode node, String parentId) {
        MenuPO byCode = menuDao.selectOne(new LambdaQueryWrapper<MenuPO>()
                .eq(MenuPO::getCode, node.getCode()).last("LIMIT 1"));
        if (byCode != null) {
            return byCode;
        }
        if (StringUtils.hasText(node.getPath())) {
            return menuDao.selectOne(new LambdaQueryWrapper<MenuPO>()
                    .eq(MenuPO::getPath, node.getPath()).last("LIMIT 1"));
        }
        if (StringUtils.hasText(node.getPerms())) {
            return menuDao.selectOne(new LambdaQueryWrapper<MenuPO>()
                    .eq(MenuPO::getPerms, node.getPerms()).last("LIMIT 1"));
        }
        LambdaQueryWrapper<MenuPO> w = new LambdaQueryWrapper<MenuPO>()
                .eq(MenuPO::getName, node.getName())
                .eq(MenuPO::getType, node.getType());
        if (parentId == null) {
            w.isNull(MenuPO::getParentId);
        } else {
            w.eq(MenuPO::getParentId, parentId);
        }
        return menuDao.selectOne(w.last("LIMIT 1"));
    }

    private String updateExisting(MenuPO po, MenuNode node, String parentId, String moduleCode) {
        po.setName(node.getName());
        po.setNameEn(node.getNameEn());
        po.setShortName(node.getShortName());
        po.setPath(node.getPath());
        po.setComponent(node.getComponent());
        po.setPerms(node.getPerms());
        po.setType(node.getType());
        po.setIcon(node.getIcon());
        po.setSort(node.getSort());
        po.setParentId(parentId);
        po.setCode(node.getCode());
        po.setModuleCode(moduleCode);
        po.setDeleted(0);
        menuDao.updateById(po);
        grantToSuperAdmin(po.getId());
        return po.getId();
    }

    private String insertNew(MenuNode node, String parentId, String moduleCode) {
        MenuPO po = new MenuPO();
        po.setParentId(parentId);
        po.setName(node.getName());
        po.setNameEn(node.getNameEn());
        po.setShortName(node.getShortName());
        po.setPath(node.getPath());
        po.setComponent(node.getComponent());
        po.setPerms(node.getPerms());
        po.setType(node.getType());
        po.setIcon(node.getIcon());
        po.setSort(node.getSort());
        po.setCode(node.getCode());
        po.setModuleCode(moduleCode);
        po.setDeleted(0);
        menuDao.insert(po);
        grantToSuperAdmin(po.getId());
        return po.getId();
    }

    /** Bind the menu to the super-admin role if not already bound. */
    private void grantToSuperAdmin(String menuId) {
        Long bound = roleMenuDao.selectCount(new LambdaQueryWrapper<RoleMenuPO>()
                .eq(RoleMenuPO::getRoleId, SUPER_ADMIN_ROLE_ID)
                .eq(RoleMenuPO::getMenuId, menuId));
        if (bound == null || bound == 0) {
            roleMenuDao.insert(new RoleMenuPO(SUPER_ADMIN_ROLE_ID, menuId));
        }
    }
}
