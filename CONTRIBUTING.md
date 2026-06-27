# Contributing to MateCloud

Thanks for your interest in contributing! This document explains how to get set
up, the conventions we follow, and how to submit changes.

## Getting started

```bash
# Backend (Java 21, Maven)
make infra-up                 # start MySQL / Redis / RabbitMQ / Nacos / MinIO
mvn clean install -DskipTests # build all modules
cd mate-biz/mate-system && mvn spring-boot:run

# Frontend (pnpm monorepo)
cd mate-ui && pnpm install && pnpm dev   # http://localhost:3000
# default dev login: admin / admin123
```

See `CLAUDE.md` and `docs/` (RFCs + conventions) for architecture details.

## Branching & commits

- Branch off `dev`. Open PRs against `dev` (not `main`).
- Use [Conventional Commits](https://www.conventionalcommits.org/):
  `feat(scope): ...`, `fix(scope): ...`, `docs:`, `refactor:`, `test:`, `chore:`.
- Keep each PR focused; one logical change per PR.

## Conventions (must follow)

- **DDD 4-layer** per business module: `trigger / application / domain / infrastructure`.
  The domain layer is framework-free (no Spring/MyBatis annotations).
- Repository interface in `domain`, implementation in `infrastructure`.
- CQRS: `CommandService` (writes) vs `QueryService` (reads) — never mixed.
- MapStruct for conversions, Lombok for boilerplate.
- Package root `vip.mate.*`; table prefix `mate_`; API prefix `/api/v1/`.
- Error codes: `{MODULE}{TYPE}{SEQ}` (e.g. `USRB001`).
- No Swagger (use Smart-Doc); no JetCache (use Spring Cache + Caffeine + Redis).

## Before you open a PR

```bash
mvn -q clean verify            # backend: compile + unit tests
cd mate-ui/apps/admin && npx vue-tsc --noEmit   # frontend type-check
```

- Add/adjust tests for the behaviour you change.
- Update `docs/` / RFCs when you change a contract or add a feature.
- Update `CHANGELOG.md` under the "Unreleased" section.
- Do not commit secrets. Never weaken security defaults (auth, tenant isolation,
  data permission) without discussion.

## Reporting bugs / requesting features

Open an issue using the templates under `.github/ISSUE_TEMPLATE`. For security
vulnerabilities, follow `SECURITY.md` instead (do **not** open a public issue).

## License

By contributing, you agree that your contributions are licensed under the
project's Apache License 2.0 (see `LICENSE`).
