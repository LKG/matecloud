-- mate-notice schema V1: Initial schema

CREATE TABLE IF NOT EXISTS `mate_notice` (
    `id`              VARCHAR(64)  NOT NULL,
    `channel`         VARCHAR(32)  NOT NULL COMMENT 'SMS/EMAIL/WECHAT',
    `target`          VARCHAR(256) NOT NULL COMMENT 'Recipient address (phone/email/openid)',
    `status`          TINYINT      NOT NULL DEFAULT 0 COMMENT '0=PENDING, 1=SUCCESS, 2=FAILED',
    `business_type`   VARCHAR(64)  NOT NULL,
    `content`         TEXT NOT NULL,
    `result_message`  VARCHAR(512) DEFAULT NULL COMMENT 'Success detail or failure reason',
    `sent_at`         DATETIME DEFAULT NULL,
    `retry_count`     INT NOT NULL DEFAULT 0,
    `max_retries`     INT NOT NULL DEFAULT 3,
    `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_channel` (`channel`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Notice records';
