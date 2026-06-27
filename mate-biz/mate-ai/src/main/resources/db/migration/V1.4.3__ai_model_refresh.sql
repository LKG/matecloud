-- =============================================================================
-- V1.4.3 — 把 V1.4.2 里 2025 旧型号刷新到 2026-05 最新可用模型。
--
-- V1.4.2 已在已部署实例落库,INSERT IGNORE 不会重跑,所以用 UPDATE
-- 补齐数据。新增 p05 智谱 GLM (V1.4.2 里没有)。
--
-- 模型版本依据:
--   Anthropic: 2026-02 推出 sonnet-4-6 / opus-4-6 (1M ctx), 04 出 opus-4-7,
--              haiku-4-5 来自 2025-10
--   OpenAI:    2026-04 推 GPT-5.5/Pro, 5.5-instant 5 月成为 ChatGPT 默认
--   Google:    Gemini 3.5 Flash 5月发布; 2.5 Pro 为官方生产推荐稳定版
--   DeepSeek:  V4 2026-04 发布,deepseek-chat/coder 名 7 月停止
--   智谱:      GLM-5 最新,GLM-4.7 编程旗舰
-- =============================================================================

-- ---- agent default_model 刷新 -----------------------------------------------
UPDATE `mate_ai_agent` SET `default_model` = 'claude-sonnet-4-6' WHERE `id` = 'a01';
UPDATE `mate_ai_agent` SET `default_model` = 'gpt-5.3-codex'     WHERE `id` = 'a02';
UPDATE `mate_ai_agent` SET `default_model` = 'gemini-2.5-pro'    WHERE `id` = 'a03';

-- ---- provider 默认/可用模型刷新 ---------------------------------------------
UPDATE `mate_ai_provider`
   SET `default_model` = 'gpt-5.5',
       `available_models` = '["gpt-5.5","gpt-5.5-pro","gpt-5.5-instant","gpt-5.3-codex","gpt-5.2"]',
       `max_tokens` = 8192
 WHERE `id` = 'p01';

UPDATE `mate_ai_provider`
   SET `default_model` = 'claude-sonnet-4-6',
       `available_models` = '["claude-opus-4-8","claude-opus-4-7","claude-sonnet-4-6","claude-haiku-4-5"]',
       `max_tokens` = 8192
 WHERE `id` = 'p02';

UPDATE `mate_ai_provider`
   SET `default_model` = 'gemini-2.5-pro',
       `available_models` = '["gemini-3.5-flash","gemini-2.5-pro","gemini-2.5-flash","gemini-2.5-flash-lite"]',
       `max_tokens` = 8192
 WHERE `id` = 'p03';

UPDATE `mate_ai_provider`
   SET `default_model` = 'deepseek-v4',
       `available_models` = '["deepseek-v4","deepseek-v4-flash","deepseek-r1"]',
       `max_tokens` = 8192
 WHERE `id` = 'p04';

-- ---- 新增 p05 智谱 GLM(V1.4.2 里没有,INSERT IGNORE 安全) -----------------
INSERT IGNORE INTO `mate_ai_provider`
  (`id`, `code`, `name`, `vendor`, `base_url`, `default_model`, `available_models`, `temperature`, `max_tokens`, `enabled`, `is_default`, `sort`)
VALUES
  ('p05', 'zhipuai', '智谱 GLM', 'ZHIPUAI', 'https://open.bigmodel.cn/api/paas/v4',
   'glm-5', '["glm-5","glm-4.7","glm-4.5","glm-4.5-air"]', 0.30, 8192, 1, 0, 5);
