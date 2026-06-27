-- mate-auth schema V1.3.1: Login log table
-- This table is written by mate-auth (LoginLogPersistenceListener)
-- and read by mate-admin (LoginLogController).
-- Uses IF NOT EXISTS for safe execution when sharing DB with mate-admin.

CREATE TABLE IF NOT EXISTS `mate_login_log` (
    `id`         VARCHAR(64) NOT NULL,
    `username`   VARCHAR(64) DEFAULT NULL,
    `client_ip`  VARCHAR(64) DEFAULT NULL,
    `user_agent` VARCHAR(512) DEFAULT NULL,
    `login_type` VARCHAR(32) DEFAULT NULL,
    `status`     TINYINT NOT NULL DEFAULT 0 COMMENT '0=success, 1=failure',
    `fail_msg`   VARCHAR(256) DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_username` (`username`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Login audit log (written by mate-auth, read by mate-admin)';
