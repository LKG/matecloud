-- =============================================================================
-- flyway-split-history.sql  —  ONE-TIME 存量库历史拆分(共享 → 每服务独立)
-- =============================================================================
-- 背景:历史上所有服务共用一张 `flyway_schema_history`,版本号是全局命名空间,
--   存在撞号(如 mate-notice 的 V1.2.1__notice_schema 与 mate-system 的
--   V1.2.1__channel_menu 同版本,主键冲突 → 其一被静默跳过)。
-- 新规范:每个服务用自己的 `flyway_history_<module-code>`(见
--   docs/conventions/pluggable-module-guide.md),版本线各自独立。
--
-- 本脚本把现有共享历史按"服务拥有的版本区间"拆进各自的表,使**已应用**的迁移
-- 不会重跑。幂等(CREATE TABLE IF NOT EXISTS + INSERT IGNORE),可重复执行。
--
-- ⚠ 执行时机:在部署"每服务独立 history"的新版本【之前】,对**已存在数据的
--   MySQL 库**跑一次。全新空库无需执行(Flyway 会从零建表)。
-- ⚠ 仅 MySQL。H2(dev/test)每次重建,不需要。
-- 用法:mysql -h <host> -u <user> -p <database> < scripts/flyway-split-history.sql
-- =============================================================================

-- mate-system — 拥有 1.0.x / 1.1.x / 1.2.x(含与 notice 撞号的 1.2.1,归 system)
CREATE TABLE IF NOT EXISTS `flyway_history_system` LIKE `flyway_schema_history`;
INSERT IGNORE INTO `flyway_history_system`
  SELECT * FROM `flyway_schema_history`
  WHERE `version` LIKE '1.0.%' OR `version` LIKE '1.1.%' OR `version` LIKE '1.2.%';

-- mate-auth — 1.3.x
CREATE TABLE IF NOT EXISTS `flyway_history_auth` LIKE `flyway_schema_history`;
INSERT IGNORE INTO `flyway_history_auth`
  SELECT * FROM `flyway_schema_history` WHERE `version` LIKE '1.3.%';

-- mate-ai — 1.4.x
CREATE TABLE IF NOT EXISTS `flyway_history_ai` LIKE `flyway_schema_history`;
INSERT IGNORE INTO `flyway_history_ai`
  SELECT * FROM `flyway_schema_history` WHERE `version` LIKE '1.4.%';

-- mate-notice — 仅它自己的 schema 迁移(精确文件名匹配,避免抓到 system 的 V1.2.1)。
--   若历史里这条因撞号被 system 顶掉而缺失,本表保持为空 → mate-notice 启动时会
--   重跑 V1.2.1__notice_schema.sql(CREATE TABLE IF NOT EXISTS,幂等安全)。
CREATE TABLE IF NOT EXISTS `flyway_history_notice` LIKE `flyway_schema_history`;
INSERT IGNORE INTO `flyway_history_notice`
  SELECT * FROM `flyway_schema_history` WHERE `script` = 'V1.2.1__notice_schema.sql';

-- 旧的 `flyway_schema_history` 不删除,仅作回滚保险;确认各服务升级正常后可改名归档:
--   RENAME TABLE `flyway_schema_history` TO `flyway_schema_history_legacy`;
-- =============================================================================
