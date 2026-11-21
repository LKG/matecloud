-- =============================================================================
-- V1.6.4 — mate-crm 条件查询(动态高级筛选) Schema
--
-- 两张支撑表(前缀 mate_crm_,可插拔规范 §1: 只 DDL 自己的表):
--   1) mate_crm_query_field  —— 各模块「可查字段」元数据白名单(驱动前端筛选器 + 后端安全翻译)
--   2) mate_crm_saved_filter —— 个人常用筛选(序列化 AdvancedQuery, 按 user_id 隔离)
-- column_name 为受信任的内部配置(管理员维护), 后端叠加白名单 + 正则双重校验; 查询值一律参数绑定。
-- =============================================================================

-- ---- 1. 可查字段元数据 ------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mate_crm_query_field` (
    `id`           VARCHAR(64)  NOT NULL,
    `tenant_id`    VARCHAR(32)  NOT NULL DEFAULT '1' COMMENT 'Tenant ID',
    `module_key`   VARCHAR(32)  NOT NULL COMMENT '模块: lead/customer/contact/followup/business/product/contract/repayment/invoice/aftersale/task',
    `field_key`    VARCHAR(64)  NOT NULL COMMENT '逻辑字段名(前端唯一标识, 如 status)',
    `column_name`  VARCHAR(64)  NOT NULL COMMENT '真实物理列名(如 status), 白名单解析',
    `label`        VARCHAR(64)  NOT NULL COMMENT '中文名(如 状态)',
    `field_type`   VARCHAR(16)  NOT NULL COMMENT '类型: TEXT/NUMBER/DATE/DATETIME/SELECT/USER/DEPT/BOOLEAN',
    `options_json` VARCHAR(2048) DEFAULT NULL COMMENT 'SELECT 可选值 [{value,label}]; 其余为 NULL',
    `sort`         INT          NOT NULL DEFAULT 0 COMMENT '展示排序',
    `visible`      TINYINT      NOT NULL DEFAULT 1 COMMENT '1=可见 0=隐藏',
    `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`      TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_qf_module_field` (`module_key`, `field_key`),
    KEY `idx_qf_module` (`module_key`),
    KEY `idx_qf_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CRM 可查字段元数据';

-- ---- 2. 个人常用筛选 --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mate_crm_saved_filter` (
    `id`          VARCHAR(64)  NOT NULL,
    `tenant_id`   VARCHAR(32)  NOT NULL DEFAULT '1' COMMENT 'Tenant ID',
    `user_id`     VARCHAR(64)  NOT NULL COMMENT '所属用户ID(仅本人可见)',
    `module_key`  VARCHAR(32)  NOT NULL COMMENT '模块: lead/customer/...',
    `name`        VARCHAR(64)  NOT NULL COMMENT '筛选方案名称',
    `query_json`  VARCHAR(2048) NOT NULL COMMENT '序列化的 AdvancedQuery(groups/order/paging)',
    `sort`        INT          NOT NULL DEFAULT 0 COMMENT '展示排序',
    `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_sf_user_module` (`user_id`, `module_key`),
    KEY `idx_sf_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CRM 个人常用筛选';
