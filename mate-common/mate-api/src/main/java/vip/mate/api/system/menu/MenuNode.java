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
package vip.mate.api.system.menu;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Flat, YAML-friendly description of a single menu node carried in a module's
 * {@code menu-manifest.yml} and registered into mate-system at startup.
 *
 * <p>The {@code code} field is the <strong>stable key</strong> — the numeric
 * {@code id} of {@code mate_menu} is assigned internally by mate-system, so
 * modules never carry or allocate ids. Parent linkage is expressed by
 * {@code parentCode} (string), not by id.</p>
 *
 * @author mateaix
 */
@Data
public class MenuNode implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Stable key (primary identity across registrations). */
    private String code;
    /** Parent's stable key; {@code null}/blank means a top-level node. */
    private String parentCode;
    private String name;
    private String nameEn;
    private String shortName;
    private String path;
    private String component;
    private String perms;
    /** M = directory, C = menu page, F = button/permission. */
    private String type;
    private String icon;
    private Integer sort;
    /** Nested children (tree form in the manifest; flattened on registration). */
    private List<MenuNode> children;
}
