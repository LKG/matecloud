# RFC-009: Admin Backend Management Features

- **Status**: Draft
- **Created**: 2026-04-12
- **Author**: MateCloud Team
- **Wave**: 4 (after Wave 1-3 scaffold complete)
- **Dependencies**: RFC-001~008 (scaffold)

## Overview

This RFC provides the complete implementation specification for the `mate-admin` module (port 9040). It covers RBAC permission management, dictionary management, and all associated DDD layers with full source code.

---

## 1. pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>vip.mate</groupId>
        <artifactId>matecloud</artifactId>
        <version>1.0.0</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>mate-admin</artifactId>
    <packaging>jar</packaging>
    <name>mate-admin</name>
    <description>MateCloud Admin Backend - RBAC, Dict, Config, Logs, Monitor</description>

    <dependencies>
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-base</artifactId>
        </dependency>
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-api</artifactId>
        </dependency>
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-ds-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-web-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-nacos-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-rpc-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-cache-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-sa-token-starter</artifactId>
            <exclusions>
                <exclusion>
                    <groupId>cn.dev33</groupId>
                    <artifactId>sa-token-reactor-spring-boot3-starter</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-monitor-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## 2. Application Entry Point

### MateAdminApplication.java

```java
package vip.mate.admin;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@EnableDubbo
public class MateAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(MateAdminApplication.class, args);
    }
}
```

---

## 3. bootstrap.yml

```yaml
server:
  port: 9040

spring:
  application:
    name: mate-admin
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER_ADDR:127.0.0.1:8848}
        namespace: ${NACOS_NAMESPACE:dev}
        group: DEFAULT_GROUP
      config:
        server-addr: ${NACOS_SERVER_ADDR:127.0.0.1:8848}
        namespace: ${NACOS_NAMESPACE:dev}
        group: DEFAULT_GROUP
        file-extension: yml
  config:
    import:
      - optional:nacos:${spring.application.name}.yml
      - optional:nacos:mate-common.yml?group=DEFAULT_GROUP&refreshEnabled=true

dubbo:
  application:
    name: mate-admin
    qos-enable: false
  protocol:
    name: dubbo
    port: -1
    serialization: fastjson2
  registry:
    address: nacos://${NACOS_SERVER_ADDR:127.0.0.1:8848}
    parameters:
      namespace: ${NACOS_NAMESPACE:dev}
  consumer:
    check: false
    timeout: 5000
    retries: 1
  scan:
    base-packages: vip.mate.admin

logging:
  level:
    vip.mate.admin: debug
```

---

## 4. SQL DDL

```sql
-- =============================================
-- mate-admin DDL
-- =============================================

CREATE TABLE `mate_admin` (
    `id`            VARCHAR(64)     NOT NULL COMMENT 'Primary key',
    `username`      VARCHAR(64)     NOT NULL COMMENT 'Login username',
    `password`      VARCHAR(128)    NOT NULL COMMENT 'BCrypt hashed password',
    `nick_name`     VARCHAR(64)     DEFAULT NULL COMMENT 'Display name',
    `avatar`        VARCHAR(512)    DEFAULT NULL COMMENT 'Avatar URL',
    `email`         VARCHAR(128)    DEFAULT NULL COMMENT 'Email address',
    `mobile`        VARCHAR(20)     DEFAULT NULL COMMENT 'Mobile number',
    `status`        TINYINT         NOT NULL DEFAULT 1 COMMENT 'Status: 1=active 0=disabled',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    `deleted`       TINYINT         NOT NULL DEFAULT 0 COMMENT 'Logical delete: 0=normal 1=deleted',
    `lock_version`  INT             NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_mobile` (`mobile`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Admin users';

CREATE TABLE `mate_role` (
    `id`            VARCHAR(64)     NOT NULL COMMENT 'Primary key',
    `role_key`      VARCHAR(64)     NOT NULL COMMENT 'Unique role key (e.g. admin, editor)',
    `role_name`     VARCHAR(64)     NOT NULL COMMENT 'Display role name',
    `sort`          INT             NOT NULL DEFAULT 0 COMMENT 'Display order',
    `status`        TINYINT         NOT NULL DEFAULT 1 COMMENT 'Status: 1=active 0=disabled',
    `data_scope`    TINYINT         NOT NULL DEFAULT 1 COMMENT 'Data scope: 1=all 2=dept 3=dept+child 4=self 5=custom',
    `remark`        VARCHAR(256)    DEFAULT NULL COMMENT 'Remark',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT         NOT NULL DEFAULT 0,
    `lock_version`  INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_key` (`role_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Roles';

CREATE TABLE `mate_menu` (
    `id`            VARCHAR(64)     NOT NULL COMMENT 'Primary key',
    `parent_id`     VARCHAR(64)     NOT NULL DEFAULT '0' COMMENT 'Parent menu ID, 0=root',
    `name`          VARCHAR(64)     NOT NULL COMMENT 'Menu name',
    `path`          VARCHAR(256)    DEFAULT NULL COMMENT 'Route path',
    `component`     VARCHAR(256)    DEFAULT NULL COMMENT 'Frontend component path',
    `perms`         VARCHAR(128)    DEFAULT NULL COMMENT 'Permission string (e.g. admin:user:list)',
    `type`          TINYINT         NOT NULL DEFAULT 1 COMMENT 'Type: 1=directory 2=menu 3=button',
    `icon`          VARCHAR(128)    DEFAULT NULL COMMENT 'Icon class name',
    `sort`          INT             NOT NULL DEFAULT 0 COMMENT 'Display order',
    `visible`       TINYINT         NOT NULL DEFAULT 1 COMMENT 'Visible: 1=show 0=hide',
    `status`        TINYINT         NOT NULL DEFAULT 1 COMMENT 'Status: 1=active 0=disabled',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT         NOT NULL DEFAULT 0,
    `lock_version`  INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Menus and permissions';

CREATE TABLE `mate_admin_role` (
    `id`            VARCHAR(64)     NOT NULL COMMENT 'Primary key',
    `admin_id`      VARCHAR(64)     NOT NULL COMMENT 'Admin ID',
    `role_id`       VARCHAR(64)     NOT NULL COMMENT 'Role ID',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_admin_role` (`admin_id`, `role_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Admin-Role mapping';

CREATE TABLE `mate_role_menu` (
    `id`            VARCHAR(64)     NOT NULL COMMENT 'Primary key',
    `role_id`       VARCHAR(64)     NOT NULL COMMENT 'Role ID',
    `menu_id`       VARCHAR(64)     NOT NULL COMMENT 'Menu ID',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`),
    KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Role-Menu mapping';

CREATE TABLE `mate_dict_type` (
    `id`            VARCHAR(64)     NOT NULL COMMENT 'Primary key',
    `dict_type`     VARCHAR(64)     NOT NULL COMMENT 'Dictionary type code',
    `dict_name`     VARCHAR(128)    NOT NULL COMMENT 'Dictionary type name',
    `status`        TINYINT         NOT NULL DEFAULT 1 COMMENT 'Status: 1=active 0=disabled',
    `remark`        VARCHAR(256)    DEFAULT NULL COMMENT 'Remark',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT         NOT NULL DEFAULT 0,
    `lock_version`  INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Dictionary types';

CREATE TABLE `mate_dict_data` (
    `id`            VARCHAR(64)     NOT NULL COMMENT 'Primary key',
    `dict_type`     VARCHAR(64)     NOT NULL COMMENT 'Dictionary type code (FK logical)',
    `dict_label`    VARCHAR(128)    NOT NULL COMMENT 'Display label',
    `dict_value`    VARCHAR(128)    NOT NULL COMMENT 'Stored value',
    `css_class`     VARCHAR(128)    DEFAULT NULL COMMENT 'CSS class for frontend',
    `sort`          INT             NOT NULL DEFAULT 0 COMMENT 'Display order',
    `status`        TINYINT         NOT NULL DEFAULT 1 COMMENT 'Status: 1=active 0=disabled',
    `remark`        VARCHAR(256)    DEFAULT NULL COMMENT 'Remark',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT         NOT NULL DEFAULT 0,
    `lock_version`  INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Dictionary data entries';

-- Seed data: super admin
INSERT INTO `mate_admin` (`id`, `username`, `password`, `nick_name`, `status`)
VALUES ('1', 'admin', '$2a$10$VUGmFv3GXxCpNqTqFgrfHeFMkEjTNQmYDC5xWzLDhPTGxqKXqKqHe', 'Super Admin', 1);

INSERT INTO `mate_role` (`id`, `role_key`, `role_name`, `sort`, `status`, `data_scope`)
VALUES ('1', 'super_admin', 'Super Administrator', 0, 1, 1);

INSERT INTO `mate_admin_role` (`id`, `admin_id`, `role_id`)
VALUES ('1', '1', '1');
```

---

## 5. Domain Layer

### 5.1 domain/permission/model/valobj/MenuType.java

```java
package vip.mate.admin.domain.permission.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MenuType {

    DIRECTORY(1, "Directory"),
    MENU(2, "Menu"),
    BUTTON(3, "Button");

    private final int code;
    private final String description;

    public static MenuType of(int code) {
        for (MenuType type : values()) {
            if (type.code == code) return type;
        }
        throw new IllegalArgumentException("Unknown MenuType code: " + code);
    }
}
```

### 5.2 domain/permission/model/valobj/AdminStatus.java

```java
package vip.mate.admin.domain.permission.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AdminStatus {

    ACTIVE(1, "Active"),
    DISABLED(0, "Disabled");

    private final int code;
    private final String description;

    public static AdminStatus of(int code) {
        for (AdminStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Unknown AdminStatus code: " + code);
    }
}
```

### 5.3 domain/permission/model/entity/Admin.java

```java
package vip.mate.admin.domain.permission.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vip.mate.admin.domain.permission.model.valobj.AdminStatus;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Admin implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String id;
    private String username;
    private String password;
    private String nickName;
    private String avatar;
    private String email;
    private String mobile;
    private AdminStatus status;
    private Date createdAt;
    private Date updatedAt;

    public void disable() {
        this.status = AdminStatus.DISABLED;
    }

    public void enable() {
        this.status = AdminStatus.ACTIVE;
    }

    public boolean isActive() {
        return this.status == AdminStatus.ACTIVE;
    }
}
```

### 5.4 domain/permission/model/entity/Role.java

```java
package vip.mate.admin.domain.permission.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String id;
    private String roleKey;
    private String roleName;
    private Integer sort;
    private Integer status;
    private Integer dataScope;
    private String remark;
    private Date createdAt;
    private Date updatedAt;

    public boolean isActive() {
        return this.status != null && this.status == 1;
    }
}
```

### 5.5 domain/permission/model/entity/Menu.java

```java
package vip.mate.admin.domain.permission.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vip.mate.admin.domain.permission.model.valobj.MenuType;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Menu implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String id;
    private String parentId;
    private String name;
    private String path;
    private String component;
    private String perms;
    private MenuType type;
    private String icon;
    private Integer sort;
    private Integer visible;
    private Integer status;
    private Date createdAt;
    private Date updatedAt;

    @Builder.Default
    private List<Menu> children = new ArrayList<>();

    public boolean isDirectory() {
        return this.type == MenuType.DIRECTORY;
    }

    public boolean isMenu() {
        return this.type == MenuType.MENU;
    }

    public boolean isButton() {
        return this.type == MenuType.BUTTON;
    }
}
```

### 5.6 domain/permission/model/aggregate/AdminAggregate.java

```java
package vip.mate.admin.domain.permission.model.aggregate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vip.mate.admin.domain.permission.model.entity.Admin;
import vip.mate.admin.domain.permission.model.entity.Role;
import vip.mate.admin.domain.permission.model.valobj.AdminStatus;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAggregate implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Admin admin;

    @Builder.Default
    private List<Role> roles = new ArrayList<>();

    public static AdminAggregate create(String username, String encodedPassword,
                                         String nickName, String email, String mobile) {
        Admin admin = Admin.builder()
                .username(username)
                .password(encodedPassword)
                .nickName(nickName)
                .email(email)
                .mobile(mobile)
                .status(AdminStatus.ACTIVE)
                .build();
        return AdminAggregate.builder().admin(admin).build();
    }

    public void assignRoles(List<Role> newRoles) {
        this.roles = newRoles != null ? new ArrayList<>(newRoles) : new ArrayList<>();
    }

    public List<String> getRoleIds() {
        return roles.stream().map(Role::getId).toList();
    }

    public List<String> getRoleKeys() {
        return roles.stream().map(Role::getRoleKey).toList();
    }

    public boolean isSuperAdmin() {
        return roles.stream().anyMatch(r -> "super_admin".equals(r.getRoleKey()));
    }
}
```

### 5.7 domain/permission/model/aggregate/RoleAggregate.java

```java
package vip.mate.admin.domain.permission.model.aggregate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vip.mate.admin.domain.permission.model.entity.Menu;
import vip.mate.admin.domain.permission.model.entity.Role;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleAggregate implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Role role;

    @Builder.Default
    private List<Menu> menus = new ArrayList<>();

    public static RoleAggregate create(String roleKey, String roleName, Integer sort, String remark) {
        Role role = Role.builder()
                .roleKey(roleKey)
                .roleName(roleName)
                .sort(sort != null ? sort : 0)
                .status(1)
                .dataScope(1)
                .remark(remark)
                .build();
        return RoleAggregate.builder().role(role).build();
    }

    public void assignMenus(List<Menu> newMenus) {
        this.menus = newMenus != null ? new ArrayList<>(newMenus) : new ArrayList<>();
    }

    public List<String> getMenuIds() {
        return menus.stream().map(Menu::getId).toList();
    }

    public List<String> getPermissions() {
        return menus.stream()
                .filter(m -> m.getPerms() != null && !m.getPerms().isBlank())
                .map(Menu::getPerms)
                .toList();
    }
}
```

### 5.8 domain/permission/adapter/repository/AdminRepository.java

```java
package vip.mate.admin.domain.permission.adapter.repository;

import vip.mate.admin.domain.permission.model.aggregate.AdminAggregate;

import java.util.List;

public interface AdminRepository {

    void save(AdminAggregate aggregate);

    void update(AdminAggregate aggregate);

    void deleteById(String id);

    AdminAggregate findById(String id);

    AdminAggregate findByUsername(String username);

    List<AdminAggregate> pageQuery(int pageNum, int pageSize, String keyword);

    long countAll(String keyword);

    void saveAdminRoles(String adminId, List<String> roleIds);

    void deleteAdminRoles(String adminId);
}
```

### 5.9 domain/permission/adapter/repository/RoleRepository.java

```java
package vip.mate.admin.domain.permission.adapter.repository;

import vip.mate.admin.domain.permission.model.aggregate.RoleAggregate;
import vip.mate.admin.domain.permission.model.entity.Role;

import java.util.List;

public interface RoleRepository {

    void save(RoleAggregate aggregate);

    void update(RoleAggregate aggregate);

    void deleteById(String id);

    RoleAggregate findById(String id);

    Role findByRoleKey(String roleKey);

    List<Role> listAll();

    List<Role> findByIds(List<String> ids);

    void saveRoleMenus(String roleId, List<String> menuIds);

    void deleteRoleMenus(String roleId);

    List<String> findRoleIdsByAdminId(String adminId);
}
```

### 5.10 domain/permission/adapter/repository/MenuRepository.java

```java
package vip.mate.admin.domain.permission.adapter.repository;

import vip.mate.admin.domain.permission.model.entity.Menu;

import java.util.List;

public interface MenuRepository {

    void save(Menu menu);

    void update(Menu menu);

    void deleteById(String id);

    Menu findById(String id);

    List<Menu> listAll();

    List<Menu> findByIds(List<String> ids);

    List<String> findMenuIdsByRoleIds(List<String> roleIds);

    List<Menu> findByRoleIds(List<String> roleIds);

    List<Menu> findChildren(String parentId);

    boolean hasChildren(String menuId);
}
```

### 5.11 domain/permission/service/IPermissionDomainService.java

```java
package vip.mate.admin.domain.permission.service;

import java.util.List;
import java.util.Set;

public interface IPermissionDomainService {

    Set<String> getPermissionsByAdminId(String adminId);

    List<String> getRoleKeysByAdminId(String adminId);

    boolean hasPermission(String adminId, String permissionCode);

    List<String> getMenuIdsByRoleIds(List<String> roleIds);
}
```

### 5.12 domain/permission/service/PermissionDomainServiceImpl.java

```java
package vip.mate.admin.domain.permission.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vip.mate.admin.domain.permission.adapter.repository.AdminRepository;
import vip.mate.admin.domain.permission.adapter.repository.MenuRepository;
import vip.mate.admin.domain.permission.adapter.repository.RoleRepository;
import vip.mate.admin.domain.permission.model.aggregate.AdminAggregate;
import vip.mate.admin.domain.permission.model.entity.Menu;
import vip.mate.admin.domain.permission.model.entity.Role;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PermissionDomainServiceImpl implements IPermissionDomainService {

    private final AdminRepository adminRepository;
    private final RoleRepository roleRepository;
    private final MenuRepository menuRepository;

    @Override
    public Set<String> getPermissionsByAdminId(String adminId) {
        AdminAggregate aggregate = adminRepository.findById(adminId);
        if (aggregate == null) return Collections.emptySet();

        // Super admin has all permissions
        if (aggregate.isSuperAdmin()) {
            List<Menu> allMenus = menuRepository.listAll();
            Set<String> perms = new HashSet<>();
            for (Menu m : allMenus) {
                if (m.getPerms() != null && !m.getPerms().isBlank()) {
                    perms.add(m.getPerms());
                }
            }
            return perms;
        }

        List<String> roleIds = aggregate.getRoleIds();
        if (roleIds.isEmpty()) return Collections.emptySet();

        List<Menu> menus = menuRepository.findByRoleIds(roleIds);
        Set<String> perms = new HashSet<>();
        for (Menu m : menus) {
            if (m.getPerms() != null && !m.getPerms().isBlank()) {
                perms.add(m.getPerms());
            }
        }
        return perms;
    }

    @Override
    public List<String> getRoleKeysByAdminId(String adminId) {
        AdminAggregate aggregate = adminRepository.findById(adminId);
        if (aggregate == null) return Collections.emptyList();
        return aggregate.getRoleKeys();
    }

    @Override
    public boolean hasPermission(String adminId, String permissionCode) {
        return getPermissionsByAdminId(adminId).contains(permissionCode);
    }

    @Override
    public List<String> getMenuIdsByRoleIds(List<String> roleIds) {
        return menuRepository.findMenuIdsByRoleIds(roleIds);
    }
}
```

### 5.13 domain/dict/model/aggregate/DictAggregate.java

```java
package vip.mate.admin.domain.dict.model.aggregate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DictAggregate implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String id;
    private String dictType;
    private String dictName;
    private Integer status;
    private String remark;
    private Date createdAt;
    private Date updatedAt;

    @Builder.Default
    private List<DictData> dataList = new ArrayList<>();

    public static DictAggregate create(String dictType, String dictName, String remark) {
        return DictAggregate.builder()
                .dictType(dictType)
                .dictName(dictName)
                .status(1)
                .remark(remark)
                .build();
    }

    public void addData(String label, String value, Integer sort) {
        DictData data = DictData.builder()
                .dictType(this.dictType)
                .dictLabel(label)
                .dictValue(value)
                .sort(sort != null ? sort : 0)
                .status(1)
                .build();
        this.dataList.add(data);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DictData implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String id;
        private String dictType;
        private String dictLabel;
        private String dictValue;
        private String cssClass;
        private Integer sort;
        private Integer status;
        private String remark;
    }
}
```

### 5.14 domain/dict/adapter/repository/DictRepository.java

```java
package vip.mate.admin.domain.dict.adapter.repository;

import vip.mate.admin.domain.dict.model.aggregate.DictAggregate;

import java.util.List;

public interface DictRepository {

    void saveType(DictAggregate aggregate);

    void updateType(DictAggregate aggregate);

    void deleteTypeById(String id);

    DictAggregate findTypeById(String id);

    DictAggregate findTypeByCode(String dictType);

    List<DictAggregate> listTypes(int pageNum, int pageSize, String keyword);

    long countTypes(String keyword);

    void saveData(DictAggregate.DictData data);

    void updateData(DictAggregate.DictData data);

    void deleteDataById(String id);

    DictAggregate.DictData findDataById(String id);

    List<DictAggregate.DictData> findDataByType(String dictType);
}
```

---

## 6. Infrastructure Layer

### 6.1 infrastructure/dao/po/AdminPO.java

```java
package vip.mate.admin.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import vip.mate.base.model.entity.BaseEntity;

import java.io.Serial;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("mate_admin")
public class AdminPO extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private String username;
    private String password;
    private String nickName;
    private String avatar;
    private String email;
    private String mobile;
    private Integer status;
}
```

### 6.2 infrastructure/dao/po/RolePO.java

```java
package vip.mate.admin.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import vip.mate.base.model.entity.BaseEntity;

import java.io.Serial;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("mate_role")
public class RolePO extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private String roleKey;
    private String roleName;
    private Integer sort;
    private Integer status;
    private Integer dataScope;
    private String remark;
}
```

### 6.3 infrastructure/dao/po/MenuPO.java

```java
package vip.mate.admin.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import vip.mate.base.model.entity.BaseEntity;

import java.io.Serial;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("mate_menu")
public class MenuPO extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private String parentId;
    private String name;
    private String path;
    private String component;
    private String perms;
    private Integer type;
    private String icon;
    private Integer sort;
    private Integer visible;
    private Integer status;
}
```

### 6.4 infrastructure/dao/po/AdminRolePO.java

```java
package vip.mate.admin.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("mate_admin_role")
public class AdminRolePO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    private String adminId;
    private String roleId;
    private Date createdAt;
}
```

### 6.5 infrastructure/dao/po/RoleMenuPO.java

```java
package vip.mate.admin.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("mate_role_menu")
public class RoleMenuPO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    private String roleId;
    private String menuId;
    private Date createdAt;
}
```

### 6.6 infrastructure/dao/po/DictTypePO.java

```java
package vip.mate.admin.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import vip.mate.base.model.entity.BaseEntity;

import java.io.Serial;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("mate_dict_type")
public class DictTypePO extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private String dictType;
    private String dictName;
    private Integer status;
    private String remark;
}
```

### 6.7 infrastructure/dao/po/DictDataPO.java

```java
package vip.mate.admin.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import vip.mate.base.model.entity.BaseEntity;

import java.io.Serial;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("mate_dict_data")
public class DictDataPO extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private String dictType;
    private String dictLabel;
    private String dictValue;
    private String cssClass;
    private Integer sort;
    private Integer status;
    private String remark;
}
```

### 6.8 infrastructure/dao/mapper/AdminMapper.java

```java
package vip.mate.admin.infrastructure.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import vip.mate.admin.infrastructure.dao.po.AdminPO;

@Mapper
public interface AdminMapper extends BaseMapper<AdminPO> {
}
```

### 6.9 infrastructure/dao/mapper/RoleMapper.java

```java
package vip.mate.admin.infrastructure.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import vip.mate.admin.infrastructure.dao.po.RolePO;

@Mapper
public interface RoleMapper extends BaseMapper<RolePO> {
}
```

### 6.10 infrastructure/dao/mapper/MenuMapper.java

```java
package vip.mate.admin.infrastructure.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import vip.mate.admin.infrastructure.dao.po.MenuPO;

@Mapper
public interface MenuMapper extends BaseMapper<MenuPO> {
}
```

### 6.11 infrastructure/dao/mapper/AdminRoleMapper.java

```java
package vip.mate.admin.infrastructure.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import vip.mate.admin.infrastructure.dao.po.AdminRolePO;

@Mapper
public interface AdminRoleMapper extends BaseMapper<AdminRolePO> {
}
```

### 6.12 infrastructure/dao/mapper/RoleMenuMapper.java

```java
package vip.mate.admin.infrastructure.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import vip.mate.admin.infrastructure.dao.po.RoleMenuPO;

@Mapper
public interface RoleMenuMapper extends BaseMapper<RoleMenuPO> {
}
```

### 6.13 infrastructure/dao/mapper/DictTypeMapper.java

```java
package vip.mate.admin.infrastructure.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import vip.mate.admin.infrastructure.dao.po.DictTypePO;

@Mapper
public interface DictTypeMapper extends BaseMapper<DictTypePO> {
}
```

### 6.14 infrastructure/dao/mapper/DictDataMapper.java

```java
package vip.mate.admin.infrastructure.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import vip.mate.admin.infrastructure.dao.po.DictDataPO;

@Mapper
public interface DictDataMapper extends BaseMapper<DictDataPO> {
}
```

### 6.15 infrastructure/adapter/repository/AdminRepositoryImpl.java

```java
package vip.mate.admin.infrastructure.adapter.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vip.mate.admin.domain.permission.adapter.repository.AdminRepository;
import vip.mate.admin.domain.permission.model.aggregate.AdminAggregate;
import vip.mate.admin.domain.permission.model.entity.Admin;
import vip.mate.admin.domain.permission.model.entity.Role;
import vip.mate.admin.domain.permission.model.valobj.AdminStatus;
import vip.mate.admin.infrastructure.dao.mapper.AdminMapper;
import vip.mate.admin.infrastructure.dao.mapper.AdminRoleMapper;
import vip.mate.admin.infrastructure.dao.mapper.RoleMapper;
import vip.mate.admin.infrastructure.dao.po.AdminPO;
import vip.mate.admin.infrastructure.dao.po.AdminRolePO;
import vip.mate.admin.infrastructure.dao.po.RolePO;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AdminRepositoryImpl implements AdminRepository {

    private final AdminMapper adminMapper;
    private final AdminRoleMapper adminRoleMapper;
    private final RoleMapper roleMapper;

    @Override
    public void save(AdminAggregate aggregate) {
        Admin admin = aggregate.getAdmin();
        AdminPO po = AdminPO.builder()
                .username(admin.getUsername())
                .password(admin.getPassword())
                .nickName(admin.getNickName())
                .avatar(admin.getAvatar())
                .email(admin.getEmail())
                .mobile(admin.getMobile())
                .status(admin.getStatus().getCode())
                .build();
        adminMapper.insert(po);
        admin.setId(po.getId());
    }

    @Override
    public void update(AdminAggregate aggregate) {
        Admin admin = aggregate.getAdmin();
        AdminPO po = adminMapper.selectById(admin.getId());
        if (po == null) return;
        po.setNickName(admin.getNickName());
        po.setAvatar(admin.getAvatar());
        po.setEmail(admin.getEmail());
        po.setMobile(admin.getMobile());
        po.setStatus(admin.getStatus().getCode());
        adminMapper.updateById(po);
    }

    @Override
    public void deleteById(String id) {
        adminMapper.deleteById(id);
        deleteAdminRoles(id);
    }

    @Override
    public AdminAggregate findById(String id) {
        AdminPO po = adminMapper.selectById(id);
        if (po == null) return null;
        return toAggregate(po);
    }

    @Override
    public AdminAggregate findByUsername(String username) {
        LambdaQueryWrapper<AdminPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdminPO::getUsername, username);
        AdminPO po = adminMapper.selectOne(wrapper);
        if (po == null) return null;
        return toAggregate(po);
    }

    @Override
    public List<AdminAggregate> pageQuery(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<AdminPO> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(AdminPO::getUsername, keyword)
                   .or().like(AdminPO::getNickName, keyword);
        }
        wrapper.orderByDesc(AdminPO::getCreatedAt);
        Page<AdminPO> page = adminMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return page.getRecords().stream().map(this::toAggregate).toList();
    }

    @Override
    public long countAll(String keyword) {
        LambdaQueryWrapper<AdminPO> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(AdminPO::getUsername, keyword)
                   .or().like(AdminPO::getNickName, keyword);
        }
        return adminMapper.selectCount(wrapper);
    }

    @Override
    public void saveAdminRoles(String adminId, List<String> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) return;
        for (String roleId : roleIds) {
            AdminRolePO arpo = AdminRolePO.builder()
                    .adminId(adminId)
                    .roleId(roleId)
                    .createdAt(new Date())
                    .build();
            adminRoleMapper.insert(arpo);
        }
    }

    @Override
    public void deleteAdminRoles(String adminId) {
        LambdaQueryWrapper<AdminRolePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdminRolePO::getAdminId, adminId);
        adminRoleMapper.delete(wrapper);
    }

    private AdminAggregate toAggregate(AdminPO po) {
        Admin admin = Admin.builder()
                .id(po.getId())
                .username(po.getUsername())
                .password(po.getPassword())
                .nickName(po.getNickName())
                .avatar(po.getAvatar())
                .email(po.getEmail())
                .mobile(po.getMobile())
                .status(AdminStatus.of(po.getStatus()))
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();

        // Load roles
        LambdaQueryWrapper<AdminRolePO> arWrapper = new LambdaQueryWrapper<>();
        arWrapper.eq(AdminRolePO::getAdminId, po.getId());
        List<AdminRolePO> arList = adminRoleMapper.selectList(arWrapper);
        List<Role> roles = new ArrayList<>();
        for (AdminRolePO ar : arList) {
            RolePO rpo = roleMapper.selectById(ar.getRoleId());
            if (rpo != null) {
                roles.add(Role.builder()
                        .id(rpo.getId())
                        .roleKey(rpo.getRoleKey())
                        .roleName(rpo.getRoleName())
                        .sort(rpo.getSort())
                        .status(rpo.getStatus())
                        .dataScope(rpo.getDataScope())
                        .remark(rpo.getRemark())
                        .build());
            }
        }
        return AdminAggregate.builder().admin(admin).roles(roles).build();
    }
}
```

### 6.16 infrastructure/adapter/repository/RoleRepositoryImpl.java

```java
package vip.mate.admin.infrastructure.adapter.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vip.mate.admin.domain.permission.adapter.repository.RoleRepository;
import vip.mate.admin.domain.permission.model.aggregate.RoleAggregate;
import vip.mate.admin.domain.permission.model.entity.Menu;
import vip.mate.admin.domain.permission.model.entity.Role;
import vip.mate.admin.domain.permission.model.valobj.MenuType;
import vip.mate.admin.infrastructure.dao.mapper.AdminRoleMapper;
import vip.mate.admin.infrastructure.dao.mapper.MenuMapper;
import vip.mate.admin.infrastructure.dao.mapper.RoleMapper;
import vip.mate.admin.infrastructure.dao.mapper.RoleMenuMapper;
import vip.mate.admin.infrastructure.dao.po.AdminRolePO;
import vip.mate.admin.infrastructure.dao.po.MenuPO;
import vip.mate.admin.infrastructure.dao.po.RoleMenuPO;
import vip.mate.admin.infrastructure.dao.po.RolePO;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class RoleRepositoryImpl implements RoleRepository {

    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final MenuMapper menuMapper;
    private final AdminRoleMapper adminRoleMapper;

    @Override
    public void save(RoleAggregate aggregate) {
        Role role = aggregate.getRole();
        RolePO po = RolePO.builder()
                .roleKey(role.getRoleKey())
                .roleName(role.getRoleName())
                .sort(role.getSort())
                .status(role.getStatus())
                .dataScope(role.getDataScope())
                .remark(role.getRemark())
                .build();
        roleMapper.insert(po);
        role.setId(po.getId());
    }

    @Override
    public void update(RoleAggregate aggregate) {
        Role role = aggregate.getRole();
        RolePO po = roleMapper.selectById(role.getId());
        if (po == null) return;
        po.setRoleName(role.getRoleName());
        po.setSort(role.getSort());
        po.setStatus(role.getStatus());
        po.setDataScope(role.getDataScope());
        po.setRemark(role.getRemark());
        roleMapper.updateById(po);
    }

    @Override
    public void deleteById(String id) {
        roleMapper.deleteById(id);
        deleteRoleMenus(id);
    }

    @Override
    public RoleAggregate findById(String id) {
        RolePO po = roleMapper.selectById(id);
        if (po == null) return null;
        return toAggregate(po);
    }

    @Override
    public Role findByRoleKey(String roleKey) {
        LambdaQueryWrapper<RolePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RolePO::getRoleKey, roleKey);
        RolePO po = roleMapper.selectOne(wrapper);
        if (po == null) return null;
        return toRole(po);
    }

    @Override
    public List<Role> listAll() {
        LambdaQueryWrapper<RolePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(RolePO::getSort);
        return roleMapper.selectList(wrapper).stream().map(this::toRole).toList();
    }

    @Override
    public List<Role> findByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) return new ArrayList<>();
        return roleMapper.selectBatchIds(ids).stream().map(this::toRole).toList();
    }

    @Override
    public void saveRoleMenus(String roleId, List<String> menuIds) {
        if (menuIds == null || menuIds.isEmpty()) return;
        for (String menuId : menuIds) {
            RoleMenuPO rmpo = RoleMenuPO.builder()
                    .roleId(roleId)
                    .menuId(menuId)
                    .createdAt(new Date())
                    .build();
            roleMenuMapper.insert(rmpo);
        }
    }

    @Override
    public void deleteRoleMenus(String roleId) {
        LambdaQueryWrapper<RoleMenuPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleMenuPO::getRoleId, roleId);
        roleMenuMapper.delete(wrapper);
    }

    @Override
    public List<String> findRoleIdsByAdminId(String adminId) {
        LambdaQueryWrapper<AdminRolePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdminRolePO::getAdminId, adminId);
        return adminRoleMapper.selectList(wrapper).stream()
                .map(AdminRolePO::getRoleId).toList();
    }

    private RoleAggregate toAggregate(RolePO po) {
        Role role = toRole(po);
        // Load menus
        LambdaQueryWrapper<RoleMenuPO> rmWrapper = new LambdaQueryWrapper<>();
        rmWrapper.eq(RoleMenuPO::getRoleId, po.getId());
        List<RoleMenuPO> rmList = roleMenuMapper.selectList(rmWrapper);
        List<Menu> menus = new ArrayList<>();
        for (RoleMenuPO rm : rmList) {
            MenuPO mpo = menuMapper.selectById(rm.getMenuId());
            if (mpo != null) {
                menus.add(toMenu(mpo));
            }
        }
        return RoleAggregate.builder().role(role).menus(menus).build();
    }

    private Role toRole(RolePO po) {
        return Role.builder()
                .id(po.getId())
                .roleKey(po.getRoleKey())
                .roleName(po.getRoleName())
                .sort(po.getSort())
                .status(po.getStatus())
                .dataScope(po.getDataScope())
                .remark(po.getRemark())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private Menu toMenu(MenuPO po) {
        return Menu.builder()
                .id(po.getId())
                .parentId(po.getParentId())
                .name(po.getName())
                .path(po.getPath())
                .component(po.getComponent())
                .perms(po.getPerms())
                .type(MenuType.of(po.getType()))
                .icon(po.getIcon())
                .sort(po.getSort())
                .visible(po.getVisible())
                .status(po.getStatus())
                .build();
    }
}
```

### 6.17 infrastructure/adapter/repository/MenuRepositoryImpl.java

```java
package vip.mate.admin.infrastructure.adapter.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vip.mate.admin.domain.permission.adapter.repository.MenuRepository;
import vip.mate.admin.domain.permission.model.entity.Menu;
import vip.mate.admin.domain.permission.model.valobj.MenuType;
import vip.mate.admin.infrastructure.dao.mapper.MenuMapper;
import vip.mate.admin.infrastructure.dao.mapper.RoleMenuMapper;
import vip.mate.admin.infrastructure.dao.po.MenuPO;
import vip.mate.admin.infrastructure.dao.po.RoleMenuPO;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class MenuRepositoryImpl implements MenuRepository {

    private final MenuMapper menuMapper;
    private final RoleMenuMapper roleMenuMapper;

    @Override
    public void save(Menu menu) {
        MenuPO po = MenuPO.builder()
                .parentId(menu.getParentId())
                .name(menu.getName())
                .path(menu.getPath())
                .component(menu.getComponent())
                .perms(menu.getPerms())
                .type(menu.getType().getCode())
                .icon(menu.getIcon())
                .sort(menu.getSort())
                .visible(menu.getVisible())
                .status(menu.getStatus())
                .build();
        menuMapper.insert(po);
        menu.setId(po.getId());
    }

    @Override
    public void update(Menu menu) {
        MenuPO po = menuMapper.selectById(menu.getId());
        if (po == null) return;
        po.setParentId(menu.getParentId());
        po.setName(menu.getName());
        po.setPath(menu.getPath());
        po.setComponent(menu.getComponent());
        po.setPerms(menu.getPerms());
        po.setType(menu.getType().getCode());
        po.setIcon(menu.getIcon());
        po.setSort(menu.getSort());
        po.setVisible(menu.getVisible());
        po.setStatus(menu.getStatus());
        menuMapper.updateById(po);
    }

    @Override
    public void deleteById(String id) {
        menuMapper.deleteById(id);
    }

    @Override
    public Menu findById(String id) {
        MenuPO po = menuMapper.selectById(id);
        return po == null ? null : toMenu(po);
    }

    @Override
    public List<Menu> listAll() {
        LambdaQueryWrapper<MenuPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(MenuPO::getSort);
        return menuMapper.selectList(wrapper).stream().map(this::toMenu).toList();
    }

    @Override
    public List<Menu> findByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) return new ArrayList<>();
        return menuMapper.selectBatchIds(ids).stream().map(this::toMenu).toList();
    }

    @Override
    public List<String> findMenuIdsByRoleIds(List<String> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) return new ArrayList<>();
        LambdaQueryWrapper<RoleMenuPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(RoleMenuPO::getRoleId, roleIds);
        return roleMenuMapper.selectList(wrapper).stream()
                .map(RoleMenuPO::getMenuId)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<Menu> findByRoleIds(List<String> roleIds) {
        List<String> menuIds = findMenuIdsByRoleIds(roleIds);
        if (menuIds.isEmpty()) return new ArrayList<>();
        return findByIds(menuIds);
    }

    @Override
    public List<Menu> findChildren(String parentId) {
        LambdaQueryWrapper<MenuPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MenuPO::getParentId, parentId).orderByAsc(MenuPO::getSort);
        return menuMapper.selectList(wrapper).stream().map(this::toMenu).toList();
    }

    @Override
    public boolean hasChildren(String menuId) {
        LambdaQueryWrapper<MenuPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MenuPO::getParentId, menuId);
        return menuMapper.selectCount(wrapper) > 0;
    }

    private Menu toMenu(MenuPO po) {
        return Menu.builder()
                .id(po.getId())
                .parentId(po.getParentId())
                .name(po.getName())
                .path(po.getPath())
                .component(po.getComponent())
                .perms(po.getPerms())
                .type(MenuType.of(po.getType()))
                .icon(po.getIcon())
                .sort(po.getSort())
                .visible(po.getVisible())
                .status(po.getStatus())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
}
```

### 6.18 infrastructure/adapter/repository/DictRepositoryImpl.java

```java
package vip.mate.admin.infrastructure.adapter.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vip.mate.admin.domain.dict.adapter.repository.DictRepository;
import vip.mate.admin.domain.dict.model.aggregate.DictAggregate;
import vip.mate.admin.infrastructure.dao.mapper.DictDataMapper;
import vip.mate.admin.infrastructure.dao.mapper.DictTypeMapper;
import vip.mate.admin.infrastructure.dao.po.DictDataPO;
import vip.mate.admin.infrastructure.dao.po.DictTypePO;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class DictRepositoryImpl implements DictRepository {

    private final DictTypeMapper dictTypeMapper;
    private final DictDataMapper dictDataMapper;

    @Override
    public void saveType(DictAggregate aggregate) {
        DictTypePO po = DictTypePO.builder()
                .dictType(aggregate.getDictType())
                .dictName(aggregate.getDictName())
                .status(aggregate.getStatus())
                .remark(aggregate.getRemark())
                .build();
        dictTypeMapper.insert(po);
        aggregate.setId(po.getId());
    }

    @Override
    public void updateType(DictAggregate aggregate) {
        DictTypePO po = dictTypeMapper.selectById(aggregate.getId());
        if (po == null) return;
        po.setDictName(aggregate.getDictName());
        po.setStatus(aggregate.getStatus());
        po.setRemark(aggregate.getRemark());
        dictTypeMapper.updateById(po);
    }

    @Override
    public void deleteTypeById(String id) {
        DictTypePO po = dictTypeMapper.selectById(id);
        if (po != null) {
            dictTypeMapper.deleteById(id);
            // Cascade delete dict data
            LambdaQueryWrapper<DictDataPO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DictDataPO::getDictType, po.getDictType());
            dictDataMapper.delete(wrapper);
        }
    }

    @Override
    public DictAggregate findTypeById(String id) {
        DictTypePO po = dictTypeMapper.selectById(id);
        if (po == null) return null;
        return toAggregate(po);
    }

    @Override
    public DictAggregate findTypeByCode(String dictType) {
        LambdaQueryWrapper<DictTypePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DictTypePO::getDictType, dictType);
        DictTypePO po = dictTypeMapper.selectOne(wrapper);
        if (po == null) return null;
        return toAggregate(po);
    }

    @Override
    public List<DictAggregate> listTypes(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<DictTypePO> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(DictTypePO::getDictName, keyword)
                   .or().like(DictTypePO::getDictType, keyword);
        }
        Page<DictTypePO> page = dictTypeMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return page.getRecords().stream().map(this::toAggregate).toList();
    }

    @Override
    public long countTypes(String keyword) {
        LambdaQueryWrapper<DictTypePO> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(DictTypePO::getDictName, keyword)
                   .or().like(DictTypePO::getDictType, keyword);
        }
        return dictTypeMapper.selectCount(wrapper);
    }

    @Override
    public void saveData(DictAggregate.DictData data) {
        DictDataPO po = DictDataPO.builder()
                .dictType(data.getDictType())
                .dictLabel(data.getDictLabel())
                .dictValue(data.getDictValue())
                .cssClass(data.getCssClass())
                .sort(data.getSort())
                .status(data.getStatus())
                .remark(data.getRemark())
                .build();
        dictDataMapper.insert(po);
        data.setId(po.getId());
    }

    @Override
    public void updateData(DictAggregate.DictData data) {
        DictDataPO po = dictDataMapper.selectById(data.getId());
        if (po == null) return;
        po.setDictLabel(data.getDictLabel());
        po.setDictValue(data.getDictValue());
        po.setCssClass(data.getCssClass());
        po.setSort(data.getSort());
        po.setStatus(data.getStatus());
        po.setRemark(data.getRemark());
        dictDataMapper.updateById(po);
    }

    @Override
    public void deleteDataById(String id) {
        dictDataMapper.deleteById(id);
    }

    @Override
    public DictAggregate.DictData findDataById(String id) {
        DictDataPO po = dictDataMapper.selectById(id);
        if (po == null) return null;
        return toDictData(po);
    }

    @Override
    public List<DictAggregate.DictData> findDataByType(String dictType) {
        LambdaQueryWrapper<DictDataPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DictDataPO::getDictType, dictType).orderByAsc(DictDataPO::getSort);
        return dictDataMapper.selectList(wrapper).stream().map(this::toDictData).toList();
    }

    private DictAggregate toAggregate(DictTypePO po) {
        List<DictAggregate.DictData> dataList = findDataByType(po.getDictType());
        return DictAggregate.builder()
                .id(po.getId())
                .dictType(po.getDictType())
                .dictName(po.getDictName())
                .status(po.getStatus())
                .remark(po.getRemark())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .dataList(dataList)
                .build();
    }

    private DictAggregate.DictData toDictData(DictDataPO po) {
        return DictAggregate.DictData.builder()
                .id(po.getId())
                .dictType(po.getDictType())
                .dictLabel(po.getDictLabel())
                .dictValue(po.getDictValue())
                .cssClass(po.getCssClass())
                .sort(po.getSort())
                .status(po.getStatus())
                .remark(po.getRemark())
                .build();
    }
}
```

---

## 7. Application Layer

### 7.1 application/command/AdminCommandService.java

```java
package vip.mate.admin.application.command;

import cn.dev33.satoken.secure.BCrypt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.mate.admin.domain.permission.adapter.repository.AdminRepository;
import vip.mate.admin.domain.permission.adapter.repository.RoleRepository;
import vip.mate.admin.domain.permission.model.aggregate.AdminAggregate;
import vip.mate.admin.domain.permission.model.entity.Role;
import vip.mate.admin.types.exception.AdminErrorCode;
import vip.mate.base.exception.BizException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminCommandService {

    private final AdminRepository adminRepository;
    private final RoleRepository roleRepository;

    @Transactional
    public String createAdmin(String username, String password, String nickName,
                               String email, String mobile, List<String> roleIds) {
        // Check uniqueness
        if (adminRepository.findByUsername(username) != null) {
            throw new BizException(AdminErrorCode.ADMIN_USERNAME_DUPLICATE);
        }
        String encodedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        AdminAggregate aggregate = AdminAggregate.create(username, encodedPassword, nickName, email, mobile);

        adminRepository.save(aggregate);

        if (roleIds != null && !roleIds.isEmpty()) {
            List<Role> roles = roleRepository.findByIds(roleIds);
            aggregate.assignRoles(roles);
            adminRepository.saveAdminRoles(aggregate.getAdmin().getId(), roleIds);
        }

        return aggregate.getAdmin().getId();
    }

    @Transactional
    public void updateAdmin(String id, String nickName, String email,
                             String mobile, List<String> roleIds) {
        AdminAggregate aggregate = adminRepository.findById(id);
        if (aggregate == null) {
            throw new BizException(AdminErrorCode.ADMIN_NOT_FOUND);
        }
        aggregate.getAdmin().setNickName(nickName);
        aggregate.getAdmin().setEmail(email);
        aggregate.getAdmin().setMobile(mobile);
        adminRepository.update(aggregate);

        // Reassign roles
        adminRepository.deleteAdminRoles(id);
        if (roleIds != null && !roleIds.isEmpty()) {
            adminRepository.saveAdminRoles(id, roleIds);
        }
    }

    @Transactional
    public void resetPassword(String id, String newPassword) {
        AdminAggregate aggregate = adminRepository.findById(id);
        if (aggregate == null) {
            throw new BizException(AdminErrorCode.ADMIN_NOT_FOUND);
        }
        aggregate.getAdmin().setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        adminRepository.update(aggregate);
    }

    @Transactional
    public void changeStatus(String id, Integer status) {
        AdminAggregate aggregate = adminRepository.findById(id);
        if (aggregate == null) {
            throw new BizException(AdminErrorCode.ADMIN_NOT_FOUND);
        }
        if (status == 1) {
            aggregate.getAdmin().enable();
        } else {
            aggregate.getAdmin().disable();
        }
        adminRepository.update(aggregate);
    }

    @Transactional
    public void deleteAdmin(String id) {
        AdminAggregate aggregate = adminRepository.findById(id);
        if (aggregate == null) {
            throw new BizException(AdminErrorCode.ADMIN_NOT_FOUND);
        }
        if (aggregate.isSuperAdmin()) {
            throw new BizException(AdminErrorCode.CANNOT_DELETE_SUPER_ADMIN);
        }
        adminRepository.deleteById(id);
    }
}
```

### 7.2 application/command/RoleCommandService.java

```java
package vip.mate.admin.application.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.mate.admin.domain.permission.adapter.repository.RoleRepository;
import vip.mate.admin.domain.permission.model.aggregate.RoleAggregate;
import vip.mate.admin.types.exception.AdminErrorCode;
import vip.mate.base.exception.BizException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleCommandService {

    private final RoleRepository roleRepository;

    @Transactional
    public String createRole(String roleKey, String roleName, Integer sort, String remark) {
        if (roleRepository.findByRoleKey(roleKey) != null) {
            throw new BizException(AdminErrorCode.ROLE_KEY_DUPLICATE);
        }
        RoleAggregate aggregate = RoleAggregate.create(roleKey, roleName, sort, remark);
        roleRepository.save(aggregate);
        return aggregate.getRole().getId();
    }

    @Transactional
    public void updateRole(String id, String roleName, Integer sort,
                            Integer dataScope, String remark) {
        RoleAggregate aggregate = roleRepository.findById(id);
        if (aggregate == null) {
            throw new BizException(AdminErrorCode.ROLE_NOT_FOUND);
        }
        aggregate.getRole().setRoleName(roleName);
        aggregate.getRole().setSort(sort);
        aggregate.getRole().setDataScope(dataScope);
        aggregate.getRole().setRemark(remark);
        roleRepository.update(aggregate);
    }

    @Transactional
    public void assignMenus(String roleId, List<String> menuIds) {
        RoleAggregate aggregate = roleRepository.findById(roleId);
        if (aggregate == null) {
            throw new BizException(AdminErrorCode.ROLE_NOT_FOUND);
        }
        roleRepository.deleteRoleMenus(roleId);
        roleRepository.saveRoleMenus(roleId, menuIds);
    }

    @Transactional
    public void deleteRole(String id) {
        RoleAggregate aggregate = roleRepository.findById(id);
        if (aggregate == null) {
            throw new BizException(AdminErrorCode.ROLE_NOT_FOUND);
        }
        roleRepository.deleteById(id);
    }
}
```

### 7.3 application/command/DictCommandService.java

```java
package vip.mate.admin.application.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.mate.admin.domain.dict.adapter.repository.DictRepository;
import vip.mate.admin.domain.dict.model.aggregate.DictAggregate;
import vip.mate.admin.types.exception.AdminErrorCode;
import vip.mate.base.exception.BizException;
import vip.mate.starter.cache.RedissonService;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DictCommandService {

    private static final String DICT_CACHE_PREFIX = "dict:";

    private final DictRepository dictRepository;
    private final RedissonService redissonService;

    @Transactional
    public String createDictType(String dictType, String dictName, String remark) {
        if (dictRepository.findTypeByCode(dictType) != null) {
            throw new BizException(AdminErrorCode.DICT_TYPE_DUPLICATE);
        }
        DictAggregate aggregate = DictAggregate.create(dictType, dictName, remark);
        dictRepository.saveType(aggregate);
        return aggregate.getId();
    }

    @Transactional
    public void updateDictType(String id, String dictName, Integer status, String remark) {
        DictAggregate aggregate = dictRepository.findTypeById(id);
        if (aggregate == null) {
            throw new BizException(AdminErrorCode.DICT_TYPE_NOT_FOUND);
        }
        aggregate.setDictName(dictName);
        aggregate.setStatus(status);
        aggregate.setRemark(remark);
        dictRepository.updateType(aggregate);
        // Invalidate cache
        redissonService.delete(DICT_CACHE_PREFIX + aggregate.getDictType());
    }

    @Transactional
    public void deleteDictType(String id) {
        DictAggregate aggregate = dictRepository.findTypeById(id);
        if (aggregate == null) {
            throw new BizException(AdminErrorCode.DICT_TYPE_NOT_FOUND);
        }
        dictRepository.deleteTypeById(id);
        redissonService.delete(DICT_CACHE_PREFIX + aggregate.getDictType());
    }

    @Transactional
    public String createDictData(String dictType, String label, String value,
                                  String cssClass, Integer sort, String remark) {
        DictAggregate.DictData data = DictAggregate.DictData.builder()
                .dictType(dictType)
                .dictLabel(label)
                .dictValue(value)
                .cssClass(cssClass)
                .sort(sort != null ? sort : 0)
                .status(1)
                .remark(remark)
                .build();
        dictRepository.saveData(data);
        redissonService.delete(DICT_CACHE_PREFIX + dictType);
        return data.getId();
    }

    @Transactional
    public void updateDictData(String id, String label, String value,
                                String cssClass, Integer sort, String remark) {
        DictAggregate.DictData data = dictRepository.findDataById(id);
        if (data == null) {
            throw new BizException(AdminErrorCode.DICT_DATA_NOT_FOUND);
        }
        data.setDictLabel(label);
        data.setDictValue(value);
        data.setCssClass(cssClass);
        data.setSort(sort);
        data.setRemark(remark);
        dictRepository.updateData(data);
        redissonService.delete(DICT_CACHE_PREFIX + data.getDictType());
    }

    @Transactional
    public void deleteDictData(String id) {
        DictAggregate.DictData data = dictRepository.findDataById(id);
        if (data == null) {
            throw new BizException(AdminErrorCode.DICT_DATA_NOT_FOUND);
        }
        dictRepository.deleteDataById(id);
        redissonService.delete(DICT_CACHE_PREFIX + data.getDictType());
    }

    public void refreshCache(String dictType) {
        List<DictAggregate.DictData> dataList = dictRepository.findDataByType(dictType);
        List<Map<String, String>> cacheList = dataList.stream()
                .map(d -> Map.of("label", d.getDictLabel(), "value", d.getDictValue()))
                .toList();
        redissonService.set(DICT_CACHE_PREFIX + dictType, cacheList);
    }
}
```

### 7.4 application/query/IAdminQueryService.java

```java
package vip.mate.admin.application.query;

import vip.mate.admin.domain.permission.model.aggregate.AdminAggregate;

import java.util.List;

public interface IAdminQueryService {

    AdminAggregate findById(String id);

    AdminAggregate findByUsername(String username);

    List<AdminAggregate> pageQuery(int pageNum, int pageSize, String keyword);

    long countAll(String keyword);
}
```

### 7.5 application/query/AdminQueryServiceImpl.java

```java
package vip.mate.admin.application.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vip.mate.admin.domain.permission.adapter.repository.AdminRepository;
import vip.mate.admin.domain.permission.model.aggregate.AdminAggregate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminQueryServiceImpl implements IAdminQueryService {

    private final AdminRepository adminRepository;

    @Override
    public AdminAggregate findById(String id) {
        return adminRepository.findById(id);
    }

    @Override
    public AdminAggregate findByUsername(String username) {
        return adminRepository.findByUsername(username);
    }

    @Override
    public List<AdminAggregate> pageQuery(int pageNum, int pageSize, String keyword) {
        return adminRepository.pageQuery(pageNum, pageSize, keyword);
    }

    @Override
    public long countAll(String keyword) {
        return adminRepository.countAll(keyword);
    }
}
```

### 7.6 application/query/IRoleQueryService.java

```java
package vip.mate.admin.application.query;

import vip.mate.admin.domain.permission.model.aggregate.RoleAggregate;
import vip.mate.admin.domain.permission.model.entity.Role;

import java.util.List;

public interface IRoleQueryService {

    RoleAggregate findById(String id);

    List<Role> listAll();

    List<String> getMenuIdsByRoleId(String roleId);
}
```

### 7.7 application/query/RoleQueryServiceImpl.java

```java
package vip.mate.admin.application.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vip.mate.admin.domain.permission.adapter.repository.RoleRepository;
import vip.mate.admin.domain.permission.model.aggregate.RoleAggregate;
import vip.mate.admin.domain.permission.model.entity.Role;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleQueryServiceImpl implements IRoleQueryService {

    private final RoleRepository roleRepository;

    @Override
    public RoleAggregate findById(String id) {
        return roleRepository.findById(id);
    }

    @Override
    public List<Role> listAll() {
        return roleRepository.listAll();
    }

    @Override
    public List<String> getMenuIdsByRoleId(String roleId) {
        RoleAggregate agg = roleRepository.findById(roleId);
        return agg != null ? agg.getMenuIds() : List.of();
    }
}
```

### 7.8 application/query/IMenuQueryService.java

```java
package vip.mate.admin.application.query;

import vip.mate.admin.domain.permission.model.entity.Menu;

import java.util.List;

public interface IMenuQueryService {

    Menu findById(String id);

    List<Menu> listAll();

    List<Menu> buildTree(List<Menu> menus);

    List<Menu> getMenuTree();

    List<Menu> getMenuTreeByRoleIds(List<String> roleIds);
}
```

### 7.9 application/query/MenuQueryServiceImpl.java

```java
package vip.mate.admin.application.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vip.mate.admin.domain.permission.adapter.repository.MenuRepository;
import vip.mate.admin.domain.permission.model.entity.Menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuQueryServiceImpl implements IMenuQueryService {

    private final MenuRepository menuRepository;

    @Override
    public Menu findById(String id) {
        return menuRepository.findById(id);
    }

    @Override
    public List<Menu> listAll() {
        return menuRepository.listAll();
    }

    @Override
    public List<Menu> buildTree(List<Menu> menus) {
        Map<String, List<Menu>> parentMap = menus.stream()
                .collect(Collectors.groupingBy(Menu::getParentId));
        for (Menu menu : menus) {
            List<Menu> children = parentMap.getOrDefault(menu.getId(), new ArrayList<>());
            menu.setChildren(children);
        }
        return menus.stream()
                .filter(m -> "0".equals(m.getParentId()))
                .toList();
    }

    @Override
    public List<Menu> getMenuTree() {
        return buildTree(listAll());
    }

    @Override
    public List<Menu> getMenuTreeByRoleIds(List<String> roleIds) {
        List<Menu> menus = menuRepository.findByRoleIds(roleIds);
        return buildTree(menus);
    }
}
```

---

## 8. Trigger Layer (Controllers)

### 8.1 trigger/controller/AdminController.java

```java
package vip.mate.admin.trigger.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vip.mate.admin.application.command.AdminCommandService;
import vip.mate.admin.application.query.IAdminQueryService;
import vip.mate.admin.domain.permission.model.aggregate.AdminAggregate;
import vip.mate.base.result.Result;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admins")
@RequiredArgsConstructor
public class AdminController {

    private final AdminCommandService adminCommandService;
    private final IAdminQueryService adminQueryService;

    @SaCheckPermission("admin:admin:create")
    @PostMapping
    public Result<String> create(@Valid @RequestBody CreateAdminRequest req) {
        return Result.ok(adminCommandService.createAdmin(
                req.username, req.password, req.nickName,
                req.email, req.mobile, req.roleIds));
    }

    @SaCheckPermission("admin:admin:update")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id,
                                @Valid @RequestBody UpdateAdminRequest req) {
        adminCommandService.updateAdmin(id, req.nickName, req.email, req.mobile, req.roleIds);
        return Result.ok();
    }

    @SaCheckPermission("admin:admin:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        adminCommandService.deleteAdmin(id);
        return Result.ok();
    }

    @SaCheckPermission("admin:admin:query")
    @GetMapping("/{id}")
    public Result<AdminAggregate> getById(@PathVariable String id) {
        return Result.ok(adminQueryService.findById(id));
    }

    @SaCheckPermission("admin:admin:query")
    @GetMapping
    public Result<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        List<AdminAggregate> list = adminQueryService.pageQuery(pageNum, pageSize, keyword);
        long total = adminQueryService.countAll(keyword);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return Result.ok(result);
    }

    @SaCheckPermission("admin:admin:resetPwd")
    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable String id,
                                       @RequestParam @NotBlank String newPassword) {
        adminCommandService.resetPassword(id, newPassword);
        return Result.ok();
    }

    @SaCheckPermission("admin:admin:update")
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable String id,
                                      @RequestParam Integer status) {
        adminCommandService.changeStatus(id, status);
        return Result.ok();
    }

    public record CreateAdminRequest(
            @NotBlank String username,
            @NotBlank String password,
            String nickName,
            String email,
            String mobile,
            List<String> roleIds
    ) {}

    public record UpdateAdminRequest(
            String nickName,
            String email,
            String mobile,
            List<String> roleIds
    ) {}
}
```

### 8.2 trigger/controller/RoleController.java

```java
package vip.mate.admin.trigger.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vip.mate.admin.application.command.RoleCommandService;
import vip.mate.admin.application.query.IRoleQueryService;
import vip.mate.admin.domain.permission.model.aggregate.RoleAggregate;
import vip.mate.admin.domain.permission.model.entity.Role;
import vip.mate.base.result.Result;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleCommandService roleCommandService;
    private final IRoleQueryService roleQueryService;

    @SaCheckPermission("admin:role:create")
    @PostMapping
    public Result<String> create(@Valid @RequestBody CreateRoleRequest req) {
        return Result.ok(roleCommandService.createRole(req.roleKey, req.roleName, req.sort, req.remark));
    }

    @SaCheckPermission("admin:role:update")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id,
                                @Valid @RequestBody UpdateRoleRequest req) {
        roleCommandService.updateRole(id, req.roleName, req.sort, req.dataScope, req.remark);
        return Result.ok();
    }

    @SaCheckPermission("admin:role:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        roleCommandService.deleteRole(id);
        return Result.ok();
    }

    @SaCheckPermission("admin:role:query")
    @GetMapping("/{id}")
    public Result<RoleAggregate> getById(@PathVariable String id) {
        return Result.ok(roleQueryService.findById(id));
    }

    @SaCheckPermission("admin:role:query")
    @GetMapping
    public Result<List<Role>> listAll() {
        return Result.ok(roleQueryService.listAll());
    }

    @SaCheckPermission("admin:role:assign")
    @PutMapping("/{id}/menus")
    public Result<Void> assignMenus(@PathVariable String id,
                                     @RequestBody List<String> menuIds) {
        roleCommandService.assignMenus(id, menuIds);
        return Result.ok();
    }

    @SaCheckPermission("admin:role:query")
    @GetMapping("/{id}/menuIds")
    public Result<List<String>> getMenuIds(@PathVariable String id) {
        return Result.ok(roleQueryService.getMenuIdsByRoleId(id));
    }

    public record CreateRoleRequest(
            @NotBlank String roleKey,
            @NotBlank String roleName,
            Integer sort,
            String remark
    ) {}

    public record UpdateRoleRequest(
            String roleName,
            Integer sort,
            Integer dataScope,
            String remark
    ) {}
}
```

### 8.3 trigger/controller/MenuController.java

```java
package vip.mate.admin.trigger.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vip.mate.admin.application.query.IMenuQueryService;
import vip.mate.admin.domain.permission.adapter.repository.MenuRepository;
import vip.mate.admin.domain.permission.model.entity.Menu;
import vip.mate.admin.domain.permission.model.valobj.MenuType;
import vip.mate.admin.types.exception.AdminErrorCode;
import vip.mate.base.exception.BizException;
import vip.mate.base.result.Result;

import java.util.List;

@RestController
@RequestMapping("/api/v1/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuRepository menuRepository;
    private final IMenuQueryService menuQueryService;

    @SaCheckPermission("admin:menu:create")
    @PostMapping
    public Result<String> create(@Valid @RequestBody CreateMenuRequest req) {
        Menu menu = Menu.builder()
                .parentId(req.parentId != null ? req.parentId : "0")
                .name(req.name)
                .path(req.path)
                .component(req.component)
                .perms(req.perms)
                .type(MenuType.of(req.type))
                .icon(req.icon)
                .sort(req.sort != null ? req.sort : 0)
                .visible(req.visible != null ? req.visible : 1)
                .status(1)
                .build();
        menuRepository.save(menu);
        return Result.ok(menu.getId());
    }

    @SaCheckPermission("admin:menu:update")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id,
                                @Valid @RequestBody UpdateMenuRequest req) {
        Menu menu = menuRepository.findById(id);
        if (menu == null) {
            throw new BizException(AdminErrorCode.MENU_NOT_FOUND);
        }
        menu.setParentId(req.parentId);
        menu.setName(req.name);
        menu.setPath(req.path);
        menu.setComponent(req.component);
        menu.setPerms(req.perms);
        menu.setType(MenuType.of(req.type));
        menu.setIcon(req.icon);
        menu.setSort(req.sort);
        menu.setVisible(req.visible);
        menuRepository.update(menu);
        return Result.ok();
    }

    @SaCheckPermission("admin:menu:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        if (menuRepository.hasChildren(id)) {
            throw new BizException(AdminErrorCode.MENU_HAS_CHILDREN);
        }
        menuRepository.deleteById(id);
        return Result.ok();
    }

    @SaCheckPermission("admin:menu:query")
    @GetMapping("/{id}")
    public Result<Menu> getById(@PathVariable String id) {
        return Result.ok(menuQueryService.findById(id));
    }

    @SaCheckPermission("admin:menu:query")
    @GetMapping("/tree")
    public Result<List<Menu>> tree() {
        return Result.ok(menuQueryService.getMenuTree());
    }

    @SaCheckPermission("admin:menu:query")
    @GetMapping
    public Result<List<Menu>> listAll() {
        return Result.ok(menuQueryService.listAll());
    }

    public record CreateMenuRequest(
            String parentId,
            @NotBlank String name,
            String path,
            String component,
            String perms,
            @NotNull Integer type,
            String icon,
            Integer sort,
            Integer visible
    ) {}

    public record UpdateMenuRequest(
            String parentId,
            @NotBlank String name,
            String path,
            String component,
            String perms,
            @NotNull Integer type,
            String icon,
            Integer sort,
            Integer visible
    ) {}
}
```

### 8.4 trigger/controller/DictController.java

```java
package vip.mate.admin.trigger.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vip.mate.admin.application.command.DictCommandService;
import vip.mate.admin.domain.dict.adapter.repository.DictRepository;
import vip.mate.admin.domain.dict.model.aggregate.DictAggregate;
import vip.mate.base.result.Result;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dicts")
@RequiredArgsConstructor
public class DictController {

    private final DictCommandService dictCommandService;
    private final DictRepository dictRepository;

    // ============== Dict Type ==============

    @SaCheckPermission("admin:dict:create")
    @PostMapping("/types")
    public Result<String> createType(@Valid @RequestBody CreateDictTypeRequest req) {
        return Result.ok(dictCommandService.createDictType(req.dictType, req.dictName, req.remark));
    }

    @SaCheckPermission("admin:dict:update")
    @PutMapping("/types/{id}")
    public Result<Void> updateType(@PathVariable String id,
                                    @Valid @RequestBody UpdateDictTypeRequest req) {
        dictCommandService.updateDictType(id, req.dictName, req.status, req.remark);
        return Result.ok();
    }

    @SaCheckPermission("admin:dict:delete")
    @DeleteMapping("/types/{id}")
    public Result<Void> deleteType(@PathVariable String id) {
        dictCommandService.deleteDictType(id);
        return Result.ok();
    }

    @SaCheckPermission("admin:dict:query")
    @GetMapping("/types")
    public Result<Map<String, Object>> pageTypes(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        List<DictAggregate> list = dictRepository.listTypes(pageNum, pageSize, keyword);
        long total = dictRepository.countTypes(keyword);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return Result.ok(result);
    }

    @SaCheckPermission("admin:dict:query")
    @GetMapping("/types/{id}")
    public Result<DictAggregate> getType(@PathVariable String id) {
        return Result.ok(dictRepository.findTypeById(id));
    }

    // ============== Dict Data ==============

    @SaCheckPermission("admin:dict:create")
    @PostMapping("/data")
    public Result<String> createData(@Valid @RequestBody CreateDictDataRequest req) {
        return Result.ok(dictCommandService.createDictData(
                req.dictType, req.label, req.value, req.cssClass, req.sort, req.remark));
    }

    @SaCheckPermission("admin:dict:update")
    @PutMapping("/data/{id}")
    public Result<Void> updateData(@PathVariable String id,
                                    @Valid @RequestBody UpdateDictDataRequest req) {
        dictCommandService.updateDictData(id, req.label, req.value, req.cssClass, req.sort, req.remark);
        return Result.ok();
    }

    @SaCheckPermission("admin:dict:delete")
    @DeleteMapping("/data/{id}")
    public Result<Void> deleteData(@PathVariable String id) {
        dictCommandService.deleteDictData(id);
        return Result.ok();
    }

    @SaCheckPermission("admin:dict:query")
    @GetMapping("/data")
    public Result<List<DictAggregate.DictData>> listData(@RequestParam @NotBlank String dictType) {
        return Result.ok(dictRepository.findDataByType(dictType));
    }

    @PostMapping("/cache/refresh")
    public Result<Void> refreshCache(@RequestParam @NotBlank String dictType) {
        dictCommandService.refreshCache(dictType);
        return Result.ok();
    }

    public record CreateDictTypeRequest(
            @NotBlank String dictType,
            @NotBlank String dictName,
            String remark
    ) {}

    public record UpdateDictTypeRequest(
            String dictName,
            Integer status,
            String remark
    ) {}

    public record CreateDictDataRequest(
            @NotBlank String dictType,
            @NotBlank String label,
            @NotBlank String value,
            String cssClass,
            Integer sort,
            String remark
    ) {}

    public record UpdateDictDataRequest(
            String label,
            String value,
            String cssClass,
            Integer sort,
            String remark
    ) {}
}
```

---

## 9. Trigger Layer (RPC)

### 9.1 trigger/rpc/RpcDictServiceImpl.java

```java
package vip.mate.admin.trigger.rpc;

import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import vip.mate.admin.application.command.DictCommandService;
import vip.mate.admin.domain.dict.adapter.repository.DictRepository;
import vip.mate.admin.domain.dict.model.aggregate.DictAggregate;
import vip.mate.api.rpc.RpcConstants;
import vip.mate.api.system.service.IRpcDictService;
import vip.mate.base.result.Result;

import java.util.List;
import java.util.Map;

@DubboService(version = RpcConstants.VERSION, group = "admin")
@RequiredArgsConstructor
public class RpcDictServiceImpl implements IRpcDictService {

    private final DictRepository dictRepository;
    private final DictCommandService dictCommandService;

    @Override
    public Result<String> getDictValue(String dictType, String dictCode) {
        List<DictAggregate.DictData> dataList = dictRepository.findDataByType(dictType);
        for (DictAggregate.DictData data : dataList) {
            if (data.getDictValue().equals(dictCode)) {
                return Result.ok(data.getDictLabel());
            }
        }
        return Result.ok(null);
    }

    @Override
    public Result<List<Map<String, String>>> getDictListByType(String dictType) {
        List<DictAggregate.DictData> dataList = dictRepository.findDataByType(dictType);
        List<Map<String, String>> result = dataList.stream()
                .map(d -> Map.of("label", d.getDictLabel(), "value", d.getDictValue()))
                .toList();
        return Result.ok(result);
    }

    @Override
    public Result<Boolean> refreshDictCache(String dictType) {
        dictCommandService.refreshCache(dictType);
        return Result.ok(true);
    }
}
```

### 9.2 trigger/rpc/RpcPermissionServiceImpl.java

```java
package vip.mate.admin.trigger.rpc;

import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import vip.mate.admin.domain.permission.adapter.repository.RoleRepository;
import vip.mate.admin.domain.permission.service.IPermissionDomainService;
import vip.mate.api.admin.service.IRpcPermissionService;
import vip.mate.api.rpc.RpcConstants;
import vip.mate.base.result.Result;

import java.util.ArrayList;
import java.util.List;

@DubboService(version = RpcConstants.VERSION, group = "admin")
@RequiredArgsConstructor
public class RpcPermissionServiceImpl implements IRpcPermissionService {

    private final IPermissionDomainService permissionDomainService;
    private final RoleRepository roleRepository;

    @Override
    public Result<List<String>> getPermissionsByRoleIds(List<String> roleIds) {
        List<String> menuIds = permissionDomainService.getMenuIdsByRoleIds(roleIds);
        return Result.ok(menuIds);
    }

    @Override
    public Result<List<String>> getRoleKeysByAdminId(String adminId) {
        List<String> roleKeys = permissionDomainService.getRoleKeysByAdminId(adminId);
        return Result.ok(roleKeys);
    }
}
```

---

## 10. mate-api Shared Contracts

### 10.1 vip.mate.api.admin.service.IRpcPermissionService

```java
package vip.mate.api.admin.service;

import vip.mate.base.result.Result;

import java.util.List;

/**
 * Dubbo RPC interface for permission operations.
 */
public interface IRpcPermissionService {

    Result<List<String>> getPermissionsByRoleIds(List<String> roleIds);

    Result<List<String>> getRoleKeysByAdminId(String adminId);
}
```

---

## 11. Types Layer

### 11.1 types/exception/AdminErrorCode.java

```java
package vip.mate.admin.types.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import vip.mate.base.exception.ErrorCode;

@Getter
@AllArgsConstructor
public enum AdminErrorCode implements ErrorCode {

    ADMIN_NOT_FOUND("A0001", "Admin not found"),
    ADMIN_USERNAME_DUPLICATE("A0002", "Username already exists"),
    CANNOT_DELETE_SUPER_ADMIN("A0003", "Cannot delete super admin"),
    ROLE_NOT_FOUND("A0010", "Role not found"),
    ROLE_KEY_DUPLICATE("A0011", "Role key already exists"),
    MENU_NOT_FOUND("A0020", "Menu not found"),
    MENU_HAS_CHILDREN("A0021", "Menu has child nodes, delete children first"),
    DICT_TYPE_NOT_FOUND("A0030", "Dict type not found"),
    DICT_TYPE_DUPLICATE("A0031", "Dict type code already exists"),
    DICT_DATA_NOT_FOUND("A0032", "Dict data not found");

    private final String code;
    private final String message;
}
```

### 11.2 types/exception/AdminException.java

```java
package vip.mate.admin.types.exception;

import vip.mate.base.exception.BizException;

import java.io.Serial;

public class AdminException extends BizException {

    @Serial
    private static final long serialVersionUID = 1L;

    public AdminException(AdminErrorCode errorCode) {
        super(errorCode);
    }

    public AdminException(AdminErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
```

---

## 12. Verification Plan

1. **RBAC**: Create role -> assign menus -> create admin -> assign roles -> login -> verify `@SaCheckPermission` enforcement
2. **Dictionary**: Create dict type -> add dict data entries -> RPC query via Dubbo -> verify cache in Redis
3. **Logs**: Call admin API -> query operation log records
4. **Gateway routing**: Access mate-admin through Gateway on path `/admin/**`
