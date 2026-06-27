-- =============================================================================
-- V1.4.1 — mate-ai schema (Spring AI 2 chat + agent + MCP + provider)
--
-- Five tables that back the AI assistant suite:
--   mate_ai_conversation  — chat session header (one row per workspace tab)
--   mate_ai_message       — message log inside a conversation (chat history)
--   mate_ai_agent         — registered CLI agent (Claude Code / Codex / ...)
--   mate_ai_mcp_server    — Model-Context-Protocol server config
--   mate_ai_provider      — LLM provider entry (model + endpoint + encrypted key)
--
-- All tables use VARCHAR(64) snowflake IDs to match the rest of MateCloud and
-- the standard audit columns (created_at / updated_at / deleted).
-- =============================================================================

CREATE TABLE IF NOT EXISTS `mate_ai_conversation` (
    `id`            VARCHAR(64)  NOT NULL,
    `tenant_id`     VARCHAR(64)  DEFAULT NULL,
    `owner_id`      VARCHAR(64)  NOT NULL COMMENT 'Owning admin id',
    `owner_name`    VARCHAR(64)  DEFAULT NULL COMMENT 'Owning admin display name (denormalized)',
    `title`         VARCHAR(256) NOT NULL DEFAULT '新会话',
    `agent_id`      VARCHAR(64)  DEFAULT NULL COMMENT 'Active CLI agent ref (mate_ai_agent.id)',
    `agent_code`    VARCHAR(64)  DEFAULT NULL COMMENT 'Cached agent code for display',
    `provider_id`   VARCHAR(64)  DEFAULT NULL COMMENT 'Active provider ref (mate_ai_provider.id)',
    `model`         VARCHAR(128) DEFAULT NULL COMMENT 'Active model name',
    `system_prompt` TEXT         DEFAULT NULL,
    `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '0=ACTIVE, 1=ARCHIVED',
    `message_count` INT          NOT NULL DEFAULT 0,
    `last_active_at` DATETIME    DEFAULT NULL,
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_owner` (`owner_id`),
    KEY `idx_tenant_owner` (`tenant_id`, `owner_id`),
    KEY `idx_last_active` (`last_active_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI conversation header';

CREATE TABLE IF NOT EXISTS `mate_ai_message` (
    `id`              VARCHAR(64) NOT NULL,
    `conversation_id` VARCHAR(64) NOT NULL,
    `role`            VARCHAR(16) NOT NULL COMMENT 'USER/ASSISTANT/SYSTEM/TOOL',
    `content`         MEDIUMTEXT  NOT NULL,
    `tool_name`       VARCHAR(128) DEFAULT NULL COMMENT 'For TOOL role: tool invoked',
    `tool_payload`    TEXT         DEFAULT NULL COMMENT 'JSON args / output snippet',
    `prompt_tokens`   INT          DEFAULT NULL,
    `completion_tokens` INT        DEFAULT NULL,
    `total_tokens`    INT          DEFAULT NULL,
    `latency_ms`      INT          DEFAULT NULL,
    `finish_reason`   VARCHAR(32)  DEFAULT NULL,
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_conversation` (`conversation_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI conversation message log';

CREATE TABLE IF NOT EXISTS `mate_ai_agent` (
    `id`            VARCHAR(64)  NOT NULL,
    `code`          VARCHAR(64)  NOT NULL COMMENT 'Stable code, e.g. claude-code / codex / gemini',
    `name`          VARCHAR(128) NOT NULL,
    `name_en`       VARCHAR(128) DEFAULT NULL,
    `category`      VARCHAR(32)  NOT NULL DEFAULT 'CODING' COMMENT 'CODING/CHAT/REVIEW',
    `provider`      VARCHAR(64)  DEFAULT NULL COMMENT 'Vendor: anthropic / openai / google / ...',
    `description`   VARCHAR(512) DEFAULT NULL,
    `icon`          VARCHAR(64)  DEFAULT NULL,
    `install_cmd`   VARCHAR(512) DEFAULT NULL COMMENT 'CLI install command, informational only',
    `launch_cmd`    VARCHAR(512) DEFAULT NULL COMMENT 'CLI launch entrypoint, informational only',
    `default_model` VARCHAR(128) DEFAULT NULL,
    `system_prompt` TEXT         DEFAULT NULL,
    `enabled`       TINYINT      NOT NULL DEFAULT 1,
    `built_in`      TINYINT      NOT NULL DEFAULT 0 COMMENT 'Cannot be deleted',
    `sort`          INT          NOT NULL DEFAULT 0,
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI CLI agent registry';

CREATE TABLE IF NOT EXISTS `mate_ai_mcp_server` (
    `id`            VARCHAR(64)  NOT NULL,
    `code`          VARCHAR(64)  NOT NULL COMMENT 'Stable code, e.g. filesystem / git / github',
    `name`          VARCHAR(128) NOT NULL,
    `transport`     VARCHAR(16)  NOT NULL DEFAULT 'STDIO' COMMENT 'STDIO/SSE/HTTP',
    `command`       VARCHAR(512) DEFAULT NULL COMMENT 'For STDIO transport',
    `args`          TEXT         DEFAULT NULL COMMENT 'JSON array of CLI args',
    `endpoint`      VARCHAR(512) DEFAULT NULL COMMENT 'For SSE/HTTP transport',
    `env_json`      TEXT         DEFAULT NULL COMMENT 'JSON object: extra env vars',
    `description`   VARCHAR(512) DEFAULT NULL,
    `tool_count`    INT          NOT NULL DEFAULT 0,
    `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '0=DISABLED, 1=ENABLED, 2=ERROR',
    `last_check_at` DATETIME     DEFAULT NULL,
    `last_error`    VARCHAR(512) DEFAULT NULL,
    `sort`          INT          NOT NULL DEFAULT 0,
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP server config';

CREATE TABLE IF NOT EXISTS `mate_ai_provider` (
    `id`             VARCHAR(64)  NOT NULL,
    `code`           VARCHAR(64)  NOT NULL COMMENT 'Stable code, e.g. openai / anthropic / google / deepseek',
    `name`           VARCHAR(128) NOT NULL,
    `vendor`         VARCHAR(64)  NOT NULL COMMENT 'OPENAI/ANTHROPIC/GOOGLE/DEEPSEEK/CUSTOM',
    `base_url`       VARCHAR(512) DEFAULT NULL,
    `api_key_cipher` VARCHAR(1024) DEFAULT NULL COMMENT 'AES-encrypted API key, never returned plaintext',
    `default_model`  VARCHAR(128) DEFAULT NULL,
    `available_models` TEXT       DEFAULT NULL COMMENT 'JSON array of supported model ids',
    `temperature`    DECIMAL(3,2) DEFAULT NULL,
    `max_tokens`     INT          DEFAULT NULL,
    `enabled`        TINYINT      NOT NULL DEFAULT 1,
    `is_default`     TINYINT      NOT NULL DEFAULT 0,
    `last_test_at`   DATETIME     DEFAULT NULL,
    `last_test_ok`   TINYINT      DEFAULT NULL,
    `sort`           INT          NOT NULL DEFAULT 0,
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`        TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LLM provider config with encrypted API key';
