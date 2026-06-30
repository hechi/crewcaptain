# Contributing to CrewCaptain

Thanks for your interest in improving CrewCaptain. This document covers how to
get a development environment running, the conventions the project follows, and
what a good pull request looks like.

CrewCaptain is licensed under the **GNU AGPL-3.0**. By contributing, you agree
that your contributions are licensed under the same terms.

---

## Getting started

See the [README](README.md) for full setup instructions. The short version:

```bash
# Backend (Java 21, PostgreSQL)
./dev.sh backend

# Frontend (Node.js 20+)
./dev.sh frontend
```

For authentication and AI during development, the bundled overlays give you a
self-contained authentik and Ollama instance:

```bash
docker compose -f docker-compose.yml \
               -f docker-compose.dev-auth.yml \
               -f docker-compose.ai.yml up
```

Copy `.env.example` to `.env` and fill in values before running the stack.
**Never commit `.env`** — it is gitignored for a reason.

---

## Architecture & conventions

CrewCaptain follows **hexagonal architecture (ports & adapters)** on the
backend. The most important rule:

- **Dependencies flow inward.** No framework or infrastructure code in the
  `domain` layer. Ports are split into `port/input/` (use case interfaces) and
  `port/output/` (repository + external service interfaces). ArchUnit tests
  enforce these boundaries — they will fail the build if violated.

Security invariants that must never be broken:

- **Every query is scoped by `userId`.** Managers must never be able to read or
  write another manager's data. Cross-manager resource access returns `404`.
- **Unauthenticated requests return `401`.** All `/api/v1/` endpoints require
  `Authorization: Bearer <jwt>`.
- **Sensitive content is respected.** Fields marked `sensitive=true` are
  encrypted at rest (when `ENCRYPTION_KEY` is set) and excluded from exports,
  AI calls (in Privacy Mode), and search snippets.

Other notes:

- Migrations are managed by **Flyway**. Add new migrations; never edit or delete
  existing ones.
- Tests use **Testcontainers** (real PostgreSQL), not H2.

---

## Development workflow

1. **Branch** from `main` using a descriptive name:
   `feat/<short-description>`, `fix/<short-description>`, `chore/...`,
   `docs/...`, or `style/...`.
2. **Write tests first.** All layers (domain, API, frontend) should be covered.
   No behavioral change should land without a corresponding test.
3. **Implement** the change with the security invariants above in mind.
4. **Run the full suite** and make sure it passes:
   ```bash
   cd api && ./gradlew test
   cd frontend && npm test
   ```
5. **Update docs** — `README.md` for user-facing features/config, and
   `api/ARCHITECTURE.md` for structural changes.
6. **Commit** using Conventional Commits (see below).

---

## Commit messages

Use [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>
```

- **Types:** `feat`, `fix`, `chore`, `docs`, `style`, `refactor`, `test`.
- Keep the subject under ~70 characters, imperative mood.
- Example: `feat(api): add per-person markdown export`

---

## Pull requests

A good PR:

- Targets `main` and is focused on a single concern.
- Has all tests passing locally and in CI.
- Includes tests for new behavior.
- Updates relevant documentation.
- Has a clear description: what changed, why, and how it was tested.

CI must be green before a PR is merged. The pipeline runs the backend and
frontend test suites on every push and pull request.

---

## Reporting bugs & requesting features

Open an issue with as much detail as you can: steps to reproduce, expected vs.
actual behavior, and your environment. For anything security-related, please
follow [SECURITY.md](SECURITY.md) instead of opening a public issue.
