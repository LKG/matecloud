# Security Policy

## Supported Versions

MateCloud is pre-1.0; security fixes land on the latest `main`/`dev`. Pin a
commit if you need stability and watch the repository for advisories.

## Reporting a Vulnerability

**Please do not open a public issue for security vulnerabilities.**

Email **security@mate.vip** with:

- A description of the issue and its impact
- Steps to reproduce (PoC if possible)
- Affected module/version (commit hash)

We aim to acknowledge within 72 hours and to provide a remediation timeline
after triage. Please give us a reasonable window to release a fix before any
public disclosure.

## Hardening checklist for deployers

MateCloud ships with **dev-only defaults** that MUST be overridden in production
via environment variables:

| Setting | Env var | Note |
|---------|---------|------|
| Nacos password | `NACOS_PASSWORD` | default is a dev value — change it |
| Sa-Token JWT secret | `SA_TOKEN_JWT_SECRET` | min 32 chars; `openssl rand -base64 48` |
| DB credentials | `spring.datasource.*` (Nacos `mate-infra-*`) | never commit real secrets |
| App-sign key | `mate_app_key` seed | rotate the built-in `mate-internal` key |

Security-relevant features and their safe defaults:

- **Multi-tenant isolation** (`mate.tenant.*`): default OFF. When enabled, tenant
  ids are validated (allow-list), row/datasource access is **fail-closed**, and
  the super tenant is disabled by default.
- **Data permission** (`@DataPermission`): scope ids are validated and quoted to
  prevent SQL injection; unknown context fails closed.
- Keep `spring.web.resources.add-mappings=false` and gateway auth enabled.

Do not weaken these defaults without understanding the impact.
