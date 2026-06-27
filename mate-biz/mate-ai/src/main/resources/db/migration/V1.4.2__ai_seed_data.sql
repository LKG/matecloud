-- =============================================================================
-- V1.4.2 — Seed CLI agents + MCP servers + sample providers for mate-ai.
-- All inserts are idempotent (INSERT IGNORE).
-- =============================================================================

-- ---- Built-in CLI agents -----------------------------------------------------
-- default_model 用 2026-05 最新可用稳定型号(每家厂商往上压一档,优先编码场景):
--   claude-code   → claude-sonnet-4-6  (Sonnet 4.6 在编码评测首次超过上一代 Opus)
--   codex         → gpt-5.3-codex      (OpenAI 最新专用编码模型)
--   gemini        → gemini-2.5-pro     (官方推荐生产稳定版,3.5-flash 太新)
INSERT IGNORE INTO `mate_ai_agent`
  (`id`, `code`, `name`, `name_en`, `category`, `provider`, `description`, `icon`, `default_model`, `built_in`, `sort`)
VALUES
  ('a01', 'claude-code', 'Claude Code',  'Claude Code',  'CODING', 'anthropic', 'Anthropic 官方编码助手, 支持终端 / IDE / MCP', 'cc',  'claude-sonnet-4-6', 1, 1),
  ('a02', 'codex',       'Codex CLI',    'Codex CLI',    'CODING', 'openai',    'OpenAI Codex, 强项是代码生成与重构',           'cx',  'gpt-5.3-codex',     1, 2),
  ('a03', 'gemini',      'Gemini CLI',   'Gemini CLI',   'CODING', 'google',    'Google Gemini 大上下文代码助手',                  'gm',  'gemini-2.5-pro',    1, 3),
  ('a04', 'opencode',    'OpenCode',     'OpenCode',     'CODING', 'custom',    '开源多模型编码代理',                              'oc',  '',                  1, 4),
  ('a05', 'amp',         'Amp',          'Amp',          'CODING', 'custom',    'Sourcegraph Amp, 仓库级搜索 + 代理',              'ap',  '',                  1, 5);

-- ---- Built-in MCP servers ----------------------------------------------------
INSERT IGNORE INTO `mate_ai_mcp_server`
  (`id`, `code`, `name`, `transport`, `command`, `args`, `description`, `tool_count`, `status`, `sort`)
VALUES
  ('m01', 'filesystem', 'Filesystem',  'STDIO', 'npx', '["-y","@modelcontextprotocol/server-filesystem","/workspace"]',          '本地文件读写工具',     6, 1, 1),
  ('m02', 'git',        'Git',         'STDIO', 'uvx', '["mcp-server-git","--repository","/workspace"]',                         'Git 仓库操作工具',     5, 1, 2),
  ('m03', 'database',   'Database',    'STDIO', 'npx', '["-y","@modelcontextprotocol/server-postgres","postgres://localhost"]',  '关系型数据库查询工具', 4, 0, 3),
  ('m04', 'github',     'GitHub',      'STDIO', 'npx', '["-y","@modelcontextprotocol/server-github"]',                           'GitHub API 工具',     2, 1, 4);

-- ---- Built-in providers (NO API keys; admin fills via UI) --------------------
-- 2026-05 各家最新可用模型清单, default_model 选最适合通用 chat / 编码场景的稳定型号。
INSERT IGNORE INTO `mate_ai_provider`
  (`id`, `code`, `name`, `vendor`, `base_url`, `default_model`, `available_models`, `temperature`, `max_tokens`, `enabled`, `is_default`, `sort`)
VALUES
  ('p01', 'openai',    'OpenAI',     'OPENAI',    'https://api.openai.com/v1',                 'gpt-5.5',            '["gpt-5.5","gpt-5.5-pro","gpt-5.5-instant","gpt-5.3-codex","gpt-5.2"]',         0.30, 8192, 1, 0, 1),
  ('p02', 'anthropic', 'Anthropic',  'ANTHROPIC', 'https://api.anthropic.com',                 'claude-sonnet-4-6',  '["claude-opus-4-8","claude-opus-4-7","claude-sonnet-4-6","claude-haiku-4-5"]', 0.30, 8192, 1, 1, 2),
  ('p03', 'google',    'Google',     'GOOGLE',    'https://generativelanguage.googleapis.com', 'gemini-2.5-pro',     '["gemini-3.5-flash","gemini-2.5-pro","gemini-2.5-flash","gemini-2.5-flash-lite"]', 0.30, 8192, 1, 0, 3),
  ('p04', 'deepseek',  'DeepSeek',   'DEEPSEEK',  'https://api.deepseek.com',                  'deepseek-v4',        '["deepseek-v4","deepseek-v4-flash","deepseek-r1"]',                              0.30, 8192, 1, 0, 4),
  ('p05', 'zhipuai',   '智谱 GLM',   'ZHIPUAI',   'https://open.bigmodel.cn/api/paas/v4',      'glm-5',              '["glm-5","glm-4.7","glm-4.5","glm-4.5-air"]',                                    0.30, 8192, 1, 0, 5);
