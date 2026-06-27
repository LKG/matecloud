# Changelog

All notable changes to this project are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/), and this project aims to follow
[Semantic Versioning](https://semver.org/) once it reaches 1.0.

## [Unreleased]

### Added
- **Spring Boot 4.0.7** + Spring Cloud 2025.1.2 + Spring Cloud Alibaba 2025.1.0.0.
- **Spring AI 2.0 integration** (`mate-ai-starter`) — `@Tool` auto-discovery,
  6 LLM providers (Anthropic / OpenAI / GLM / Minimax / DeepSeek / Ollama),
  MCP Server bridge, conversation memory.
- **mate-ai service** (port 9030) — AI chat, conversation management, model
  provider CRUD with encrypted API keys (AES-256-GCM).
- **mate-cli** — Picocli CLI for scaffolding, Nacos config, service discovery,
  health checks, code generation, and MCP stdio server.
- **mate-notice service** (port 9050) — SMS / email notification adapters.
- **Department management** — DDD 4-layer CRUD + tree, admin tree UI, menu seed.
- **Data permission** — role data scope (ALL / DEPT / DEPT_AND_CHILD / SELF /
  CUSTOM), `@DataPermission` filtering with table-targeting.
- **Tenant package management** — package CRUD API + admin UI.
- **Multi-tenant SCHEMA / DATASOURCE routing foundation** — dynamic-datasource
  integration, per-request datasource routing, `TenantHelper`.
- **SSO starter** (`mate-sso-starter`) — LDAP / CAS / OAuth2 identity providers.
- **Model starter** (`mate-model-starter`) — multi-model factory (chat, embedding, TTS).
- **Vue 3 frontend** (`mate-ui`) — pnpm monorepo, Element Plus, TypeScript,
  shared packages (core, hooks, ui, utils).
- **VitePress documentation site** — 30 pages covering architecture, starters,
  CLI, frontend, AI, and deployment.
- Open-source governance: `LICENSE` (Apache 2.0), `CONTRIBUTING.md`,
  `CODE_OF_CONDUCT.md`, `SECURITY.md`, issue / PR templates, this changelog.

### Changed
- **mate-admin merged into mate-system** (RFC-049) — single service on port 9030
  handles RBAC, dict, config, logs, and DDD showcase.
- Removed mate-hive, mate-aigc, mate-publish, mate-ocr (commercial modules).
- README rewritten in Chinese.

### Fixed
- **Multi-tenant row-level isolation** — `TenantLineInnerInterceptor` properly
  collected into MyBatis-Plus chain; whitelist-based table selection; fail-closed
  on missing/invalid tenant context.
- Tenant id validation at trust boundary (web / gateway / RPC) — prevents SQL
  injection / tenant bypass.
- DataScopeInterceptor: alphanumeric id validation, quoted literals, fail-closed.
- Dubbo provider tenant filter: rejects malformed attachments, fail-closed for
  non-`@CrossTenantRpc` calls, thread-context cleanup.
- Admin enable/disable and edit dialogs: status type + `MateForm` reseed bug.

### Security
- Super tenant disabled by default; datasource-name resolution fails closed for
  spoofed tenant ids. See `SECURITY.md` for the deployer hardening checklist.
