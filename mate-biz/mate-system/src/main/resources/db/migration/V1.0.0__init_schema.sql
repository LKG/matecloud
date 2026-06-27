-- =============================================================================
-- V1.0.0 — mate-system 初始化 Schema（基线）
--
-- 本文件是 mate-system 的建表基线，由历史 V1.0.x~V1.3.x 多个迁移压缩而成。
-- 仅含通用 scaffold + 仓库内产品能力的表（共 25 张），不含任何下游
-- (hive/aigc/ai) 表。字符集 utf8mb4，引擎 InnoDB，无物理外键（MyBatis-Plus 风格，
-- 关系约束在应用层维护）。
--
-- 迁移规范见同目录 V1.0.2__example_add_column.sql。
-- 已发布的迁移文件不可再改（会触发 Flyway checksum 校验失败）——新增变更一律追加新的 V* 文件。
-- =============================================================================

CREATE TABLE `mate_admin` (
  `id` varchar(64) NOT NULL,
  `tenant_id` varchar(32) NOT NULL DEFAULT '1' COMMENT 'Tenant ID',
  `username` varchar(64) NOT NULL,
  `mobile` varchar(20) DEFAULT NULL,
  `email` varchar(128) DEFAULT NULL,
  `password` varchar(128) NOT NULL,
  `nick_name` varchar(64) DEFAULT NULL,
  `real_name` varchar(64) DEFAULT NULL,
  `avatar` varchar(512) DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `lock_version` int NOT NULL DEFAULT '0',
  `dept_id` varchar(32) DEFAULT NULL COMMENT '所属部门ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `mate_admin_role` (
  `admin_id` varchar(64) NOT NULL,
  `role_id` varchar(64) NOT NULL,
  PRIMARY KEY (`admin_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `mate_role` (
  `id` varchar(64) NOT NULL,
  `tenant_id` varchar(32) NOT NULL DEFAULT '1' COMMENT 'Tenant ID',
  `role_key` varchar(64) NOT NULL,
  `role_name` varchar(64) NOT NULL,
  `sort` int DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `data_scope` tinyint NOT NULL DEFAULT '1' COMMENT '数据范围:1全部 2本部门 3本部门及下级 4本人 5自定义',
  `custom_dept_ids` varchar(512) DEFAULT NULL COMMENT '自定义数据范围部门ID(逗号分隔,data_scope=5 时生效)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_key` (`role_key`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `mate_menu` (
  `id` varchar(64) NOT NULL,
  `parent_id` varchar(64) DEFAULT NULL,
  `name` varchar(64) NOT NULL,
  `name_en` varchar(64) DEFAULT NULL COMMENT 'English menu name',
  `short_name` varchar(32) DEFAULT NULL,
  `path` varchar(256) DEFAULT NULL,
  `component` varchar(256) DEFAULT NULL,
  `perms` varchar(128) DEFAULT NULL,
  `type` varchar(2) NOT NULL COMMENT 'M=dir, C=menu, F=button',
  `icon` varchar(64) DEFAULT NULL,
  `sort` int DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `code` varchar(128) DEFAULT NULL COMMENT '菜单稳定键(模块自注册用)',
  `module_code` varchar(64) DEFAULT NULL COMMENT '归属模块码',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `mate_role_menu` (
  `role_id` varchar(64) NOT NULL,
  `menu_id` varchar(64) NOT NULL,
  PRIMARY KEY (`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `mate_dept` (
  `id` varchar(32) NOT NULL COMMENT 'Department ID',
  `parent_id` varchar(32) DEFAULT '0' COMMENT 'Parent department ID',
  `dept_name` varchar(64) NOT NULL COMMENT 'Department name',
  `ancestors` varchar(512) DEFAULT '' COMMENT 'Ancestor chain (comma-separated IDs)',
  `sort` int NOT NULL DEFAULT '0' COMMENT 'Display order',
  `leader` varchar(64) DEFAULT NULL COMMENT 'Department leader',
  `phone` varchar(20) DEFAULT NULL COMMENT 'Contact phone',
  `email` varchar(64) DEFAULT NULL COMMENT 'Contact email',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT 'Status: 0=active, 1=disabled',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT 'Logical delete: 0=normal, 1=deleted',
  `lock_version` int NOT NULL DEFAULT '0' COMMENT 'Optimistic lock version',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Department table';
CREATE TABLE `mate_app_key` (
  `id` varchar(32) NOT NULL COMMENT 'Primary key',
  `app_key` varchar(64) NOT NULL COMMENT 'Application key',
  `app_secret` varchar(128) NOT NULL COMMENT 'Application secret',
  `app_name` varchar(128) NOT NULL COMMENT 'Application name',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT 'Status: 0=active, 1=disabled',
  `expire_at` datetime DEFAULT NULL COMMENT 'Expiration time (null=never)',
  `remark` varchar(256) DEFAULT NULL COMMENT 'Remark',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT 'Logical delete: 0=normal, 1=deleted',
  `lock_version` int NOT NULL DEFAULT '0' COMMENT 'Optimistic lock version',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_key` (`app_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='API key management table';
CREATE TABLE `mate_dict_type` (
  `id` varchar(64) NOT NULL,
  `dict_type` varchar(64) NOT NULL,
  `dict_name` varchar(64) NOT NULL,
  `status` tinyint NOT NULL DEFAULT '1',
  `remark` varchar(512) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `mate_dict_data` (
  `id` varchar(64) NOT NULL,
  `dict_type` varchar(64) NOT NULL,
  `dict_label` varchar(128) NOT NULL,
  `dict_value` varchar(128) NOT NULL,
  `sort` int DEFAULT '0',
  `remark` varchar(512) DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `mate_config` (
  `id` varchar(64) NOT NULL,
  `config_key` varchar(128) NOT NULL,
  `config_value` text,
  `config_name` varchar(128) DEFAULT NULL,
  `built_in` tinyint NOT NULL DEFAULT '0',
  `remark` varchar(512) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `mate_login_log` (
  `id` varchar(64) NOT NULL,
  `username` varchar(64) DEFAULT NULL,
  `client_ip` varchar(64) DEFAULT NULL,
  `user_agent` varchar(512) DEFAULT NULL,
  `login_type` varchar(32) DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT '0',
  `fail_msg` varchar(256) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_username` (`username`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `mate_operation_log` (
  `id` varchar(64) NOT NULL,
  `user_id` varchar(64) DEFAULT NULL,
  `username` varchar(64) DEFAULT NULL,
  `module` varchar(64) DEFAULT NULL,
  `operation_type` varchar(32) DEFAULT NULL,
  `description` varchar(200) DEFAULT NULL COMMENT '操作描述',
  `request_method` varchar(16) DEFAULT NULL,
  `request_url` varchar(512) DEFAULT NULL,
  `request_params` text,
  `response_result` text,
  `client_ip` varchar(64) DEFAULT NULL,
  `user_agent` varchar(500) DEFAULT NULL COMMENT 'User-Agent',
  `status` tinyint NOT NULL DEFAULT '0',
  `error_msg` varchar(512) DEFAULT NULL,
  `duration` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `mate_user` (
  `id` varchar(64) NOT NULL,
  `tenant_id` varchar(32) NOT NULL DEFAULT '1' COMMENT 'Tenant ID',
  `username` varchar(64) NOT NULL,
  `password` varchar(128) NOT NULL,
  `mobile` varchar(20) DEFAULT NULL,
  `email` varchar(128) DEFAULT NULL,
  `real_name` varchar(64) DEFAULT NULL,
  `avatar` varchar(512) DEFAULT NULL,
  `gender` tinyint DEFAULT '0',
  `status` tinyint DEFAULT '0',
  `last_login_time` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `lock_version` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_mobile` (`mobile`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `mate_user_operate_stream` (
  `id` varchar(64) NOT NULL,
  `user_id` varchar(64) NOT NULL,
  `operate_type` varchar(32) NOT NULL,
  `operate_detail` varchar(512) DEFAULT NULL,
  `operate_time` datetime NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `mate_tenant` (
  `id` varchar(32) NOT NULL COMMENT 'Tenant ID (also used as Sa-Token session attr)',
  `tenant_code` varchar(32) NOT NULL COMMENT 'Short, URL-safe tenant code (subdomain candidate)',
  `tenant_name` varchar(128) NOT NULL COMMENT 'Display name',
  `contact_name` varchar(64) DEFAULT NULL,
  `contact_phone` varchar(20) DEFAULT NULL,
  `contact_email` varchar(128) DEFAULT NULL,
  `package_id` varchar(32) NOT NULL COMMENT 'FK -> mate_tenant_package.id',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0=active 1=suspended 2=expired 3=deleted',
  `domain` varchar(255) DEFAULT NULL COMMENT 'Custom domain (optional)',
  `expire_at` datetime DEFAULT NULL COMMENT 'Expiration timestamp (null = never)',
  `remark` varchar(256) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `lock_version` int NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_code` (`tenant_code`),
  KEY `idx_status_expire` (`status`,`expire_at`),
  KEY `idx_package_id` (`package_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Tenant aggregate root';
CREATE TABLE `mate_tenant_package` (
  `id` varchar(32) NOT NULL COMMENT 'Package ID',
  `package_code` varchar(32) NOT NULL COMMENT 'Code: BASIC / PRO / ENTERPRISE / CUSTOM',
  `package_name` varchar(128) NOT NULL COMMENT 'Display name',
  `max_users` int NOT NULL DEFAULT '10' COMMENT 'Max user accounts (0 = unlimited)',
  `max_storage` bigint NOT NULL DEFAULT '0' COMMENT 'Max storage bytes (0 = unlimited)',
  `features` varchar(1024) DEFAULT NULL COMMENT 'Comma-separated feature flags',
  `ai_enabled` tinyint NOT NULL DEFAULT '0' COMMENT 'Whether AI is enabled (0/1)',
  `ai_quota_daily` int NOT NULL DEFAULT '0' COMMENT 'Daily AI call cap (0 = unlimited)',
  `max_apps` int NOT NULL DEFAULT '0' COMMENT 'Max app installs (0 = unlimited)',
  `price` int NOT NULL DEFAULT '0' COMMENT 'Monthly price in cents',
  `remark` varchar(256) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_package_code` (`package_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Tenant subscription packages';
CREATE TABLE `mate_channel_config` (
  `id` varchar(32) NOT NULL COMMENT 'PK',
  `channel` varchar(32) NOT NULL COMMENT 'Logical channel: storage / sms',
  `provider_type` varchar(32) NOT NULL COMMENT 'Provider type: minio/oss/aliyun/tencent/...',
  `config_json` text COMMENT 'Provider config as JSON (field key -> value)',
  `enabled` tinyint NOT NULL DEFAULT '0' COMMENT '1 = active provider for this channel',
  `scope` varchar(32) NOT NULL DEFAULT 'GLOBAL' COMMENT 'Tenant scope; GLOBAL for now',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_channel_type_scope` (`channel`,`provider_type`,`scope`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Channel (storage/sms) provider configuration';
CREATE TABLE `mate_material` (
  `id` varchar(32) NOT NULL COMMENT 'PK',
  `tenant_id` varchar(32) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `category_id` varchar(32) DEFAULT NULL COMMENT '分组ID(空=未分组)',
  `type` varchar(16) NOT NULL COMMENT 'image / video / doc',
  `name` varchar(255) NOT NULL COMMENT '展示名(原始文件名)',
  `object_name` varchar(512) NOT NULL COMMENT '对象存储 key',
  `url` varchar(1024) DEFAULT NULL COMMENT '直链(可能需桶公开)',
  `size` bigint NOT NULL DEFAULT '0' COMMENT '字节',
  `ext` varchar(16) DEFAULT NULL COMMENT '扩展名',
  `mime` varchar(128) DEFAULT NULL COMMENT 'Content-Type',
  `uploaded_by` varchar(64) DEFAULT NULL COMMENT '上传者 loginId',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_mat` (`tenant_id`,`type`,`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='素材';
CREATE TABLE `mate_material_category` (
  `id` varchar(32) NOT NULL COMMENT 'PK',
  `tenant_id` varchar(32) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `type` varchar(16) NOT NULL COMMENT 'image / video / doc',
  `name` varchar(64) NOT NULL COMMENT '分组名',
  `sort` int NOT NULL DEFAULT '0',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_cat_type` (`tenant_id`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='素材分组';
CREATE TABLE `mate_model_default` (
  `id` varchar(64) NOT NULL COMMENT 'PK (snowflake)',
  `tenant_id` varchar(32) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `model_type` varchar(16) NOT NULL COMMENT 'LLM/EMBEDDING/RERANK/TTS/STT/IMAGE/VIDEO/MODERATION',
  `provider_id` varchar(64) NOT NULL COMMENT 'mate_model_provider.id',
  `model` varchar(128) NOT NULL COMMENT '模型名,如 gpt-4o',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_default_type` (`tenant_id`,`model_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 模型默认指派(按类型)';
CREATE TABLE `mate_model_gateway` (
  `id` varchar(64) NOT NULL COMMENT 'PK (snowflake)',
  `tenant_id` varchar(32) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `enabled` tinyint NOT NULL DEFAULT '0' COMMENT '1 = 经网关供给',
  `gateway_type` varchar(16) NOT NULL DEFAULT 'NEW_API' COMMENT 'NEW_API/LITELLM/ONE_API/CUSTOM',
  `base_url` varchar(512) DEFAULT NULL COMMENT '网关 OpenAI 兼容 base_url',
  `token_cipher` text COMMENT '网关令牌密文(AES)',
  `default_group` varchar(64) DEFAULT NULL COMMENT '默认分组',
  `model_mapping` text COMMENT '模型映射(JSON)',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_gateway_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 模型网关配置';
CREATE TABLE `mate_model_provider` (
  `id` varchar(64) NOT NULL COMMENT 'PK (snowflake)',
  `tenant_id` varchar(32) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `scope` varchar(16) NOT NULL DEFAULT 'GLOBAL' COMMENT 'GLOBAL / TENANT',
  `vendor` varchar(32) NOT NULL COMMENT 'OPENAI/AZURE/DEEPSEEK/QWEN/OLLAMA/JINA/COHERE/AZURE_TTS/COSYVOICE/FLUX/KLING/LUMA/GATEWAY/CUSTOM',
  `name` varchar(128) NOT NULL COMMENT '展示名',
  `modalities` varchar(255) DEFAULT NULL COMMENT '逗号分隔:LLM,EMBEDDING,RERANK,TTS,STT,IMAGE,VIDEO,MODERATION',
  `config_json` text COMMENT '凭证密文(整体 AES 加密的 JSON)',
  `enabled` tinyint NOT NULL DEFAULT '1' COMMENT '1 = 启用',
  `sort` int NOT NULL DEFAULT '0',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `models` text COMMENT 'JSON 数组:可用模型 id',
  PRIMARY KEY (`id`),
  KEY `idx_provider` (`tenant_id`,`vendor`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 模型供应商配置';
CREATE TABLE `mate_identity_dept_mapping` (
  `id` varchar(32) NOT NULL COMMENT 'PK',
  `provider` varchar(32) NOT NULL COMMENT 'Provider code',
  `external_id` varchar(128) NOT NULL COMMENT 'Provider-side department id',
  `local_dept_id` varchar(32) DEFAULT NULL COMMENT 'Local department id (mate_dept)',
  `tenant_id` varchar(32) DEFAULT NULL COMMENT 'Tenant scope',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_provider_ext` (`provider`,`external_id`,`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='SSO external-dept ↔ local-dept mapping';
CREATE TABLE `mate_identity_sync_log` (
  `id` varchar(32) NOT NULL COMMENT 'PK',
  `provider` varchar(32) NOT NULL COMMENT 'Provider code',
  `object_type` varchar(16) NOT NULL DEFAULT 'both' COMMENT 'dept | user | both',
  `trigger_by` varchar(16) NOT NULL DEFAULT 'manual' COMMENT 'manual | job | webhook',
  `added` int NOT NULL DEFAULT '0',
  `updated` int NOT NULL DEFAULT '0',
  `removed` int NOT NULL DEFAULT '0',
  `result` varchar(16) NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS | FAIL',
  `error` varchar(512) DEFAULT NULL,
  `cost_ms` bigint DEFAULT NULL,
  `tenant_id` varchar(32) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_provider_created` (`provider`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='SSO organization sync audit log';
CREATE TABLE `mate_identity_user_mapping` (
  `id` varchar(32) NOT NULL COMMENT 'PK',
  `provider` varchar(32) NOT NULL COMMENT 'Provider code: wechat_work/dingtalk/feishu/ldap',
  `external_id` varchar(128) NOT NULL COMMENT 'Provider-side stable user id (join key)',
  `union_id` varchar(128) DEFAULT NULL COMMENT 'Cross-app union id (DingTalk/WeChat)',
  `principal_id` varchar(32) DEFAULT NULL COMMENT 'Local principal id (mate_admin / member id)',
  `principal_type` varchar(16) NOT NULL DEFAULT 'admin' COMMENT 'admin | member',
  `external_name` varchar(128) DEFAULT NULL COMMENT 'External display name (backup)',
  `external_mobile` varchar(32) DEFAULT NULL COMMENT 'External mobile (backup)',
  `tenant_id` varchar(32) DEFAULT NULL COMMENT 'Tenant scope (null/GLOBAL)',
  `last_sync_at` datetime DEFAULT NULL COMMENT 'Last sync time',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT 'Logical delete',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_provider_ext` (`provider`,`external_id`,`tenant_id`),
  KEY `idx_principal` (`principal_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='SSO external-user ↔ local-principal mapping';
