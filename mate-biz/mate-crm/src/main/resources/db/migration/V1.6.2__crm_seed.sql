-- =============================================================================
-- V1.6.2 — mate-crm 种子数据
--
-- 公海默认规则(单行) + 示例产品, INSERT IGNORE 保证幂等。
-- 菜单不在此处 —— 按可插拔规范 §2 由 menu-manifest.yml 启动期自注册。
-- =============================================================================

INSERT IGNORE INTO `mate_crm_pool_config`
    (`id`, `enabled`, `recycle_days`, `max_hold`, `remind_days`)
VALUES ('1', 1, 30, 200, 7);

INSERT IGNORE INTO `mate_crm_product`
    (`id`, `product_name`, `product_code`, `category`, `price`, `unit`, `status`, `description`, `sort`)
VALUES
    ('9001', 'MateCloud 基础版(年)', 'MC-BASIC-Y', 'SaaS订阅', 19800.00, '套/年', 1, '标准功能, 含 20 个坐席', 1),
    ('9002', 'MateCloud 专业版(年)', 'MC-PRO-Y', 'SaaS订阅', 49800.00, '套/年', 1, '含高级 BI 与开放 API, 50 个坐席', 2),
    ('9003', 'MateCloud 私有化部署', 'MC-PRIV', '私有化', 298000.00, '套', 1, '源码级私有化部署, 含一年实施', 3),
    ('9004', '实施服务包', 'MC-IMPL', '服务', 50000.00, '项目', 1, '标准实施 + 培训', 4),
    ('9005', 'AI 智能坐席包', 'MC-AI', '增值服务', 24000.00, '套/年', 1, 'AI 对话助手 + 知识库问答', 5);
