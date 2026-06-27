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
package vip.mate.starter.menu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.StringUtils;
import vip.mate.api.rpc.RpcConstants;
import vip.mate.api.system.menu.IRpcMenuRegistry;
import vip.mate.api.system.menu.MenuNode;
import vip.mate.base.result.Result;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * On {@link ApplicationReadyEvent}, reads {@code classpath:menu-manifest.yml} and
 * idempotently self-registers the module's menus into mate-system via Dubbo.
 *
 * <p>Fail-soft by design: mate-system may not be up yet (the reference is
 * {@code check=false}, so it can be null), so every failure path only logs a
 * WARN and never aborts host-service startup. The upsert is idempotent, so the
 * next successful boot reconciles.</p>
 *
 * @author mateaix
 */
@Slf4j
public class MenuAutoRegistrar implements ApplicationListener<ApplicationReadyEvent> {

    private static final String MANIFEST = "menu-manifest.yml";
    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_SLEEP_MS = 3000L;

    @DubboReference(check = false, timeout = RpcConstants.LONG_TIMEOUT,
            group = RpcConstants.GROUP_SYSTEM, version = RpcConstants.VERSION)
    private IRpcMenuRegistry registry;

    private final Environment environment;
    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());

    public MenuAutoRegistrar(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            register();
        } catch (Exception e) {
            log.warn("[menu] self-registration skipped due to error: {}", e.getMessage());
        }
    }

    private void register() throws Exception {
        String moduleCode = environment.getProperty("mate.module.code");
        if (!StringUtils.hasText(moduleCode)) {
            log.warn("[menu] mate.module.code is not set; skip menu self-registration");
            return;
        }
        // 聚合 classpath 上所有 jar 的 menu-manifest.yml(每个 starter / 场景包各带一份),
        // 而非只读单个 —— 否则同名资源在多 jar 中只命中其一, 导致大部分菜单丢失。
        // 再扁平化为「带 parentCode 的平铺列表」后过线: 不依赖嵌套 children 的跨进程序列化,
        // 也兼容旧版 mate-system(避免「只注册到目录、子菜单/按钮丢失」)。
        List<MenuNode> menus = flattenForWire(loadAllMenus());
        if (menus.isEmpty()) {
            log.debug("[menu] no menus found across classpath:{}; skip self-registration", MANIFEST);
            return;
        }

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                if (registry == null) {
                    throw new IllegalStateException("IRpcMenuRegistry reference is null (mate-system not reachable)");
                }
                Result<Integer> result = registry.register(moduleCode, menus);
                if (result != null && Boolean.TRUE.equals(result.getSuccess())) {
                    log.info("[menu] module {} self-registered {} nodes", moduleCode, result.getData());
                    return;
                }
                String msg = (result == null) ? "rpc returned null" : result.getMsg();
                log.warn("[menu] module {} self-registration attempt {}/{} rejected: {}",
                        moduleCode, attempt, MAX_ATTEMPTS, msg);
            } catch (Exception e) {
                log.warn("[menu] module {} self-registration attempt {}/{} failed: {}",
                        moduleCode, attempt, MAX_ATTEMPTS, e.getMessage());
            }
            if (attempt < MAX_ATTEMPTS) {
                try {
                    Thread.sleep(RETRY_SLEEP_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        log.warn("[menu] module {} self-registration gave up after {} attempts", moduleCode, MAX_ATTEMPTS);
    }

    /**
     * 聚合 classpath 上所有 jar 中的 {@code menu-manifest.yml}。
     *
     * <p>每个 starter / 场景包各自携带一份同名清单, 用 {@code classpath*:} 才能全部命中并合并;
     * 顶层节点(如同名父菜单)由 mate-system 按 {@code code} 幂等 upsert 去重。</p>
     */
    private List<MenuNode> loadAllMenus() {
        List<MenuNode> all = new ArrayList<>();
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath*:" + MANIFEST);
            for (Resource resource : resources) {
                if (!resource.exists()) {
                    continue;
                }
                try (InputStream in = resource.getInputStream()) {
                    MenuManifest manifest = yaml.readValue(in, MenuManifest.class);
                    if (manifest != null && manifest.getMenus() != null) {
                        all.addAll(manifest.getMenus());
                    }
                } catch (Exception e) {
                    log.warn("[menu] failed to parse {}: {}", resource.getDescription(), e.getMessage());
                }
            }
            log.info("[menu] aggregated {} top-level node(s) from {} manifest(s) on classpath",
                    all.size(), resources.length);
        } catch (Exception e) {
            log.warn("[menu] scan classpath*:{} failed: {}", MANIFEST, e.getMessage());
        }
        return all;
    }

    /**
     * 把嵌套菜单树扁平化为「平铺 + parentCode」列表(pre-order, 父在子前):
     * 自动补全缺省的 parentCode, 并清空 children, 使跨进程序列化只传简单对象,
     * 由 mate-system 按 parentCode 重建层级。
     */
    private List<MenuNode> flattenForWire(List<MenuNode> tree) {
        List<MenuNode> flat = new ArrayList<>();
        collect(tree, null, flat);
        return flat;
    }

    private void collect(List<MenuNode> nodes, String parentCode, List<MenuNode> out) {
        if (nodes == null) {
            return;
        }
        for (MenuNode node : nodes) {
            List<MenuNode> children = node.getChildren();
            if (parentCode != null && !StringUtils.hasText(node.getParentCode())) {
                node.setParentCode(parentCode);
            }
            node.setChildren(null);
            out.add(node);
            collect(children, node.getCode(), out);
        }
    }
}
