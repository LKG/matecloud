-- =============================================================================
-- V1.0.2 — Flyway 迁移示例与规范（可删；保留作为新增迁移的范本）
--
-- 本文件演示在 mate-system 中新增一个数据库变更的标准写法，并作为团队规范的
-- 单一参考样例。真实业务变更请「复制本文件 → 改版本号与内容」，不要修改已发布文件。
--
-- ── 规范 ──────────────────────────────────────────────────────────────────
-- 1. 文件名: V<major>.<minor>.<patch>__<snake_case_描述>.sql
--    · 版本号严格递增（Flyway 按版本排序执行），描述用小写下划线。
--    · 纯查询/可重复脚本用 R__ 前缀；本仓库统一用版本化 V__。
-- 2. 不可变性: 一旦提交且被任意环境执行过，该文件内容（含注释、空白）不可再改——
--    改动会改变 checksum，导致 Flyway 启动校验失败。需要修正/回滚，一律「追加」新的
--    V* 文件向前修复，而非编辑旧文件。
-- 3. 表/字段约定: 表名 mate_ 前缀；引擎 InnoDB，字符集 utf8mb4；不建物理外键
--    （关系约束在应用层维护，便于分库与逻辑删除）；时间列 created_at/updated_at，
--    逻辑删除列 deleted。
-- 4. 幂等性: 数据 INSERT 一律用 INSERT IGNORE（或 ON DUPLICATE KEY UPDATE）；
--    删除菜单等关联数据时「先删关联表（mate_role_menu）再删主表（mate_menu）」。
-- 5. 数据库: 生产与测试（Testcontainers）均为 MySQL 8，可直接使用 MySQL 语法。
-- ─────────────────────────────────────────────────────────────────────────
--
-- 【示例变更】给角色表增加 remark 备注列，并回填两个内置角色的备注。
-- =============================================================================

-- ① DDL：新增列（带 COMMENT；ALTER 由 Flyway 保证仅执行一次）
ALTER TABLE `mate_role`
    ADD COLUMN `remark` VARCHAR(255) DEFAULT NULL COMMENT '角色备注' AFTER `role_name`;

-- ② DML：回填——UPDATE 天然幂等，可安全重跑
UPDATE `mate_role` SET `remark` = '系统超级管理员，拥有全部权限' WHERE `role_key` = 'ROLE_ADMIN';
UPDATE `mate_role` SET `remark` = '普通用户，仅基础权限'         WHERE `role_key` = 'ROLE_USER';
