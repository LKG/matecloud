-- ========================================================================
-- mate-sso-starter — identity mapping tables (REFERENCE, NOT auto-run)
-- ========================================================================
-- Per coding-standards §9.2 and the mate-channel-starter precedent (the
-- mate_channel_config table is owned by mate-system, not by the starter), a
-- starter must NOT ship a Flyway-versioned migration: its V-numbers would
-- collide with the consuming service's own migrations.
--
-- USAGE: copy these statements into the CONSUMING service's
--   src/main/resources/db/migration/V{next}__sso_identity.sql
-- (e.g. mate-system or mate-lms), picking the next free version number.
--
-- The starter's JdbcIdentityMappingAdapter reads/writes these tables.
-- Credentials are NOT here — they live in mate_channel_config (channel='identity')
-- via mate-channel-starter (AES-encrypted, admin-managed, tenant-scoped).
-- ========================================================================

CREATE TABLE IF NOT EXISTS mate_identity_user_mapping (
    id              VARCHAR(32)  NOT NULL COMMENT 'PK',
    provider        VARCHAR(32)  NOT NULL COMMENT 'Provider code: wechat_work/dingtalk/feishu/ldap',
    external_id     VARCHAR(128) NOT NULL COMMENT 'Provider-side stable user id (join key)',
    union_id        VARCHAR(128) DEFAULT NULL COMMENT 'Cross-app union id (DingTalk/WeChat); for identity unification',
    principal_id    VARCHAR(32)  DEFAULT NULL COMMENT 'Local principal id (the consumer''s user/member id)',
    principal_type  VARCHAR(16)  NOT NULL DEFAULT 'admin' COMMENT 'admin | member — lets mate-system & mate-lms share this table',
    external_name   VARCHAR(128) DEFAULT NULL COMMENT 'External display name (backup)',
    external_mobile VARCHAR(32)  DEFAULT NULL COMMENT 'External mobile (backup)',
    tenant_id       VARCHAR(32)  DEFAULT NULL COMMENT 'Tenant scope (null/GLOBAL for single-tenant)',
    last_sync_at    DATETIME     DEFAULT NULL COMMENT 'Last sync time',
    deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT 'Logical delete',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated',
    PRIMARY KEY (id),
    UNIQUE KEY uk_provider_ext (provider, external_id, tenant_id),
    KEY idx_principal (principal_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SSO external-user ↔ local-principal mapping';

CREATE TABLE IF NOT EXISTS mate_identity_dept_mapping (
    id            VARCHAR(32)  NOT NULL COMMENT 'PK',
    provider      VARCHAR(32)  NOT NULL COMMENT 'Provider code',
    external_id   VARCHAR(128) NOT NULL COMMENT 'Provider-side department id',
    local_dept_id VARCHAR(32)  DEFAULT NULL COMMENT 'Local department id (consumer''s dept table)',
    tenant_id     VARCHAR(32)  DEFAULT NULL COMMENT 'Tenant scope',
    deleted       TINYINT      NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_provider_ext (provider, external_id, tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SSO external-dept ↔ local-dept mapping';

CREATE TABLE IF NOT EXISTS mate_identity_sync_log (
    id          VARCHAR(32)  NOT NULL COMMENT 'PK',
    provider    VARCHAR(32)  NOT NULL COMMENT 'Provider code',
    object_type VARCHAR(16)  NOT NULL COMMENT 'dept | user | both',
    trigger_by  VARCHAR(16)  NOT NULL COMMENT 'manual | job | webhook',
    added       INT          NOT NULL DEFAULT 0 COMMENT 'Members added',
    updated     INT          NOT NULL DEFAULT 0 COMMENT 'Members updated',
    removed     INT          NOT NULL DEFAULT 0 COMMENT 'Members pruned',
    result      VARCHAR(16)  NOT NULL COMMENT 'SUCCESS | FAIL',
    error       VARCHAR(512) DEFAULT NULL COMMENT 'Error message on failure',
    cost_ms     BIGINT       DEFAULT NULL COMMENT 'Duration in ms',
    tenant_id   VARCHAR(32)  DEFAULT NULL COMMENT 'Tenant scope',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_provider_created (provider, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SSO organization sync audit log';
