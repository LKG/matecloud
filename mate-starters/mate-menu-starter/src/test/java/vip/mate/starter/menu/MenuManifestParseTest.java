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
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import vip.mate.api.system.menu.MenuNode;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 menu-manifest.yml 经 classpath*: 聚合解析后, 嵌套 children (C/F) 不丢失,
 * 且 pre-order flatten 能展开出菜单与按钮 —— 复现/守护「只剩目录」问题。
 *
 * @author mateaix
 */
class MenuManifestParseTest {

    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());

    @Test
    void nestedChildrenSurviveParseAndFlatten() throws Exception {
        Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources("classpath*:menu-manifest.yml");
        List<MenuNode> top = new ArrayList<>();
        for (Resource r : resources) {
            try (InputStream in = r.getInputStream()) {
                MenuManifest m = yaml.readValue(in, MenuManifest.class);
                if (m != null && m.getMenus() != null) {
                    top.addAll(m.getMenus());
                }
            }
        }
        assertFalse(top.isEmpty(), "应至少解析出一个根节点");
        MenuNode parent = top.stream().filter(n -> "t_parent".equals(n.getCode())).findFirst().orElseThrow();
        assertFalse(parent.getChildren() == null || parent.getChildren().isEmpty(), "父目录的 children 不应为空");

        List<MenuNode> flat = new ArrayList<>();
        flatten(top, flat);
        assertTrue(flat.stream().anyMatch(n -> "t_menu".equals(n.getCode()) && "C".equals(n.getType())),
                "flatten 后应包含 C 菜单");
        assertTrue(flat.stream().anyMatch(n -> "t_btn".equals(n.getCode()) && "F".equals(n.getType())),
                "flatten 后应包含 F 按钮");
        assertEquals("t_parent", flat.get(0).getCode(), "pre-order: 父在子之前");
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
}
