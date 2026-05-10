# AGENTS.md — AI Agent Operating Manual

Project Name: CrewCaptain

> This file governs how AI agents (Copilot, Claude, Cursor, Codex, etc.)
> interact with this codebase. Read this file in full before making any changes.
> No exceptions.

---

## 0. Golden Rules (Non-Negotiable)

1. **No code change without a test.** Every feature, fix, or refactor must be
   accompanied by tests. PRs without tests will be rejected.
2. **Always update `README.md`** when you add, change, or remove a feature,
   configuration option, environment variable, or deployment step.
3. **Always update `PROGRESS.md`** at the end of every session or task batch.
   Future sessions depend on this file to resume work correctly.
4. **Never break the build.** Run the full test suite before considering any
   task complete.
5. **Never cross manager data boundaries.** Every query MUST be scoped by
   `userId`. This is a security invariant — treat violations as critical bugs.
6. **Ask before inventing.** If a requirement is ambiguous, state your
   assumption explicitly and flag it for human review rather than silently
   deciding.
7. **No exceptions for "small" changes.** Documentation-only edits, single-line
   fixes, config tweaks — all follow the full workflow (§3). The size of a change
   does not justify skipping branching, committing, or updating PROGRESS.md.
   If it touches a file in this repo, it gets a branch.

---

## 1. Project Overview

| Property        | Value                                      |
|-----------------|--------------------------------------------|
| Product         | People Manager Workspace                   |
| License         | AGPL-3.0                                   |
| Purpose         | Self-hosted manager-only CRM for 1:1s,     |
|                 | PDP tracking, action items, kudos          |
| Backend         | Kotlin + Spring Boot 3 (Hexagonal/DDD)     |
| Frontend        | React + Next.js + Auth.js (OIDC)           |
| Database        | PostgreSQL                                 |
| Auth            | OAuth2 / OIDC via authentik                |
| Deployment      | Docker Compose (NOT Kubernetes)            |
| API Style       | REST + JSON                                |

**Reference documents (read before coding):**
- `README.md` — setup, configuration, running locally
- `PROGRESS.md` — current state, completed tasks, known issues, next steps
- `DESIGN.md` — gives branding details, logo, color schema, font
- `docs/PRD.md` — full product requirements (source of truth for features)
- `docs/DATA_MODEL.md` — domain model and aggregate definitions
- `docs/ADR/` — Architecture Decision Records

---

## 2. Repository Structure

```
/
├── AGENTS.md                  ← You are here
├── README.md                  ← Always keep updated
├── PROGRESS.md                ← Always update at session end
├── DESIGN.md                ← Always keep the branding and style in mind
├── dev.sh                     ← Local dev runner (./dev.sh backend | frontend)
├── docker-compose.yml
├── docker-compose.override.yml (local dev, gitignored)
│
├── docs/
│   ├── PRD.md                 ← Product requirements
│   ├── DATA_MODEL.md          ← Domain model reference
│   └── ADR/                   ← Architecture Decision Records
│       └── YYYYMMDD-title.md
│
├── api/                       ← Kotlin Spring Boot backend
│   ├── src/
│   │   ├── main/kotlin/
│   │   │   └── com/peoplemanager/
│   │   │       ├── domain/        ← Aggregates, Value Objects, Domain Services
│   │   │       ├── application/   ← Use Cases (Commands/Queries), Ports
│   │   │       └── adapters/
│   │   │           ├── web/       ← REST Controllers
│   │   │           ├── persistence/ ← JPA Repositories
│   │   │           ├── auth/      ← OIDC/JWT verification
│   │   │           └── scheduler/ ← Notification generation
│   │   └── test/kotlin/
│   │       └── com/peoplemanager/
│   │           ├── domain/        ← Domain unit tests
│   │           ├── application/   ← Use case tests
│   │           ├── adapters/web/  ← Controller/integration tests
│   │           └── integration/   ← Full stack integration tests
│   ├── build.gradle.kts
│   └── Dockerfile
│
└── frontend/                  ← Next.js frontend
    ├── src/
    │   ├── app/               ← Next.js App Router pages
    │   ├── components/        ← Reusable UI components
    │   ├── lib/               ← API client, auth helpers, utilities
    │   └── types/             ← TypeScript type definitions
    ├── __tests__/             ← Jest + React Testing Library tests
    ├── e2e/                   ← Playwright end-to-end tests
    ├── package.json
    └── Dockerfile
```

---

## 3. Mandatory Workflow — Every Task

Follow this sequence for **every** task, no matter how small:

```
1. BRANCH →  Create a branch from main (see §4 for naming)
2. READ   →  Read PROGRESS.md to understand current state
3. PLAN   →  State what you will do and how you will test it
4. TEST   →  Write or update tests FIRST (TDD preferred)
5. CODE   →  Implement the change
6. VERIFY →  Run all relevant tests; confirm they pass
7. DOCS   →  Update README.md and any relevant docs/
8. LOG    →  Update PROGRESS.md
9. COMMIT →  Commit with a conventional commit message (see §4)
10. DONE  →  Declare task complete with summary
```

**Never skip steps 4, 7, 8, or 9.**

---

## 4. Git Branching & Commit Workflow

### 4.1 Branch Strategy

All work happens on feature/fix branches off `main`. Never commit directly to
`main`.

**Branch naming convention:**

| Type        | Pattern                              | Example                                  |
|-------------|--------------------------------------|------------------------------------------|
| Feature     | `feat/<short-description>`           | `feat/person-crud-api`                   |
| Bugfix      | `fix/<short-description>`            | `fix/userid-scoping-on-action-items`     |
| Refactor    | `refactor/<short-description>`       | `refactor/extract-notification-port`     |
| Chore       | `chore/<short-description>`          | `chore/upgrade-spring-boot-3.4`          |
| Docs        | `docs/<short-description>`           | `docs/add-authentik-setup-guide`         |
| Style       | `style/<short-description>`          | `style/update-dashboard-layout`          |
| Hotfix      | `hotfix/<short-description>`         | `hotfix/fix-jwt-validation-crash`        |

**Rules:**
- Use lowercase kebab-case for the description
- Keep branch names short but descriptive (max ~50 chars)
- One logical change per branch — don't mix unrelated features
- Delete branches after merge

### 4.2 Branch Lifecycle

```
main ─────────────────────────────────────────────────── main
  │                                                       ↑
  └── feat/person-crud-api ──●──●──●── (squash merge) ──┘
```

1. **Create branch** from latest `main`:
   ```bash
   git checkout main
   git pull origin main
   git checkout -b feat/<description>
   ```

2. **Work on the branch** — commit early and often with meaningful messages.

3. **Push the branch** when ready for review:
   ```bash
   git push -u origin feat/<description>
   ```

4. **Create a Pull Request** via `gh pr create` (GitHub) or equivalent.

5. **Merge** — prefer squash merge to keep `main` history clean.

6. **Delete** the branch after merge.

### 4.3 Commit Message Convention

Follow [Conventional Commits](https://www.conventionalcommits.org/) strictly:

```
<type>(<scope>): <subject>

[optional body]

[optional footer(s)]
```

**Types:**

| Type       | When to use                                          |
|------------|------------------------------------------------------|
| `feat`     | A new feature or user-facing capability               |
| `fix`      | A bug fix                                            |
| `refactor` | Code restructuring without behavior change           |
| `test`     | Adding or updating tests only                        |
| `docs`     | Documentation changes only                           |
| `chore`    | Build config, dependencies, CI, tooling              |
| `style`    | Formatting, whitespace (no logic change)             |
| `perf`     | Performance improvement                              |
| `ci`       | CI/CD pipeline changes                               |

**Scopes** (optional but encouraged):

| Scope       | Meaning                        |
|-------------|--------------------------------|
| `api`       | Backend (Spring Boot)          |
| `web`       | Frontend (Next.js)             |
| `db`        | Database / migrations          |
| `docker`    | Docker / Compose               |
| `auth`      | Authentication / OIDC          |
| `domain`    | Domain layer                   |
| `infra`     | Infrastructure / tooling       |

**Subject rules:**
- Use imperative mood: "add", "fix", "remove" — not "added", "fixes", "removed"
- Lowercase first letter, no period at the end
- Max 72 characters for the subject line
- Reference issue/ticket if applicable in the footer

**Examples:**

```bash
# Feature
feat(api): add person CRUD endpoints

Implement GET/POST/PUT/DELETE for /api/v1/persons.
All endpoints enforce userId scoping.

Refs: #12

# Bug fix
fix(api): enforce userId scoping on action item queries

Previously, findByPersonId did not filter by userId,
allowing cross-manager data access.

# Refactor
refactor(domain): extract notification scheduling to port interface

# Chore
chore(docker): upgrade postgres image from 15 to 16

# Docs
docs: add authentik OIDC setup instructions to README

# Test
test(api): add integration tests for person repository
```

### 4.4 Commit Granularity

- **One logical change per commit.** Don't mix feature code with formatting fixes.
- **Tests and implementation in the same commit** (they belong together).
- **Documentation updates** can be in the same commit if directly related to the
  code change, or a separate `docs:` commit if standalone.
- **Migrations** get their own commit: `feat(db): add persons table migration`

### 4.5 When the User Asks to Commit

When the user says "commit", "commit this", or "commit the changes":

1. **Stage only related files** — use `git add <specific files>`, not `git add .`
2. **Write a proper conventional commit message** following §4.3
3. **Include a body** if the change is non-trivial (more than a one-liner)
4. **Never amend** commits that have been pushed unless explicitly asked
5. **Never force push** unless explicitly asked

### 4.6 Pull Request Guidelines

When creating a PR (via `gh pr create` or equivalent):

- **Title**: Same format as commit subject — `<type>(<scope>): <subject>`
- **Description** must include:
  - Summary of what changed and why
  - How it was tested
  - Any breaking changes or migration steps
  - Blocked/follow-up items (if any)
- **Labels**: Apply relevant labels (feature, bugfix, docs, etc.)
- **Keep PRs focused** — one feature/fix per PR

### 4.7 Protected Branch Rules

- `main` is the stable branch — always deployable
- All changes to `main` go through pull requests
- PRs require all tests to pass before merge
- Prefer squash merge for clean history on `main`

---

## 5. Testing Requirements

### 5.1 Mandatory Coverage Rules

| Layer                     | Required Tests                              | Framework                        |
|---------------------------|---------------------------------------------|----------------------------------|
| Domain (Kotlin)           | Unit tests for all aggregates, value objects, domain services | JUnit 5 + Kotest (preferred)  |
| Application / Use Cases   | Unit tests with mocked ports                | JUnit 5 + Mockk                 |
| REST Controllers          | Slice tests (`@WebMvcTest`) for all endpoints | Spring MockMvc + JUnit 5       |
| Persistence               | Integration tests with real Postgres         | Testcontainers + JUnit 5        |
| Auth / Security           | Tests verifying JWT validation and userId scoping | Spring Security Test          |
| Frontend Components       | Unit/component tests for all new components | Jest + React Testing Library    |
| Frontend Pages            | Integration test for page-level flows       | Jest + React Testing Library    |
| End-to-End (critical paths) | Happy path for core flows                 | Playwright                      |

### 5.2 Non-Negotiable Test Cases

The following **must always have tests** — these are security and correctness invariants:

- [ ] A manager cannot read another manager's `Person` records
- [ ] A manager cannot read another manager's `1:1 Entry` records
- [ ] A manager cannot read another manager's `Action Items`
- [ ] All list endpoints return only the authenticated manager's data
- [ ] Unauthenticated requests to any `/api/**` endpoint return `401`
- [ ] Sensitive-flagged content is marked in API responses
- [ ] Notification generation does not leak cross-user data

### 5.3 Running Tests

```bash
# Backend — all tests
cd api && ./gradlew test

# Backend — specific layer
./gradlew test --tests "com.peoplemanager.domain.*"
./gradlew test --tests "com.peoplemanager.adapters.web.*"

# Backend — integration tests (requires Docker for Testcontainers)
./gradlew integrationTest

# Frontend — unit + component tests
cd frontend && npm test

# Frontend — tests with coverage
npm run test:coverage

# Frontend — e2e tests (requires running stack)
npm run test:e2e

# Full stack (CI equivalent)
docker compose -f docker-compose.test.yml up --abort-on-container-exit
```

### 5.4 Test Quality Standards

- Tests must be **meaningful** — no coverage padding with trivial assertions.
- Test names must read as specifications:
  - ✅ `should return 403 when manager requests another manager's person`
  - ❌ `test1` or `testGetPerson`
- Use the **Arrange / Act / Assert** pattern consistently.
- Mock at the port boundary (application layer), not inside domain logic.
- Testcontainers must be used for any test touching the database — no H2.

---

## 6. Architecture Rules

### 6.1 Hexagonal Architecture (Backend)

```
[Web / Auth / Scheduler Adapters]
           ↓ calls
[Application — Use Cases / Ports]
           ↓ calls
[Domain — Aggregates / Domain Services]
           ↑ implemented by
[Persistence Adapter]
```

**Dependency direction is strictly inward:**
- `domain` has ZERO framework dependencies (no Spring, no JPA annotations)
- `application` depends only on `domain` and defines port interfaces
- `adapters` depend on `application` and `domain`; never the reverse

**Violations of this rule must be flagged and corrected before merging.**

### 6.2 Data Scoping (Security Invariant)

Every repository method and use case that returns data MUST accept and enforce `userId`:

```kotlin
// ✅ Correct
fun findPersonById(userId: UserId, personId: PersonId): Person?

// ❌ Wrong — never do this
fun findPersonById(personId: PersonId): Person?
```

This must be verified at the use case layer AND the persistence layer.

### 6.3 Domain Rules to Enforce

- `Person` belongs to exactly one `User` (manager). No sharing.
- `OneOnOneEntry` is always scoped: `userId` + `personId`.
- `ActionItem` status transitions: `OPEN → DONE`, `OPEN → CANCELED` only.
- `PdpGoal` status transitions: `ACTIVE → ACHIEVED`, `ACTIVE → PAUSED`,
  `ACTIVE → DROPPED`, `PAUSED → ACTIVE`.
- `Notification` is always private to the owning `User`.
- Morale values: `GREEN | YELLOW | RED | UNKNOWN` — no freeform values.

### 6.4 API Conventions

- All endpoints require `Authorization: Bearer <jwt>` header.
- Base path: `/api/v1/`
- Pagination: `?page=0&size=20` (Spring Pageable convention).
- Error responses:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Person not found",
  "timestamp": "2025-01-01T00:00:00Z"
}
```

- Never expose internal IDs from other managers in error messages.
- Return `404` (not `403`) when a manager requests another manager's resource
  (do not confirm existence).

---

## 7. Frontend Rules

### 7.1 Auth

- Use **Auth.js (NextAuth)** with the OIDC provider pointed at authentik.
- The access token must be forwarded as `Authorization: Bearer` to all API calls.
- Never store tokens in `localStorage` — use httpOnly cookies via Auth.js.
- Wrap all authenticated pages in the session guard.

### 7.2 Component Standards

- All new components must have a co-located test file:
  `ComponentName.tsx` → `ComponentName.test.tsx`
- Use TypeScript strictly — no `any` types without an explanatory comment.
- Define API response types in `src/types/` — never use inline `any` for API data.
- Sensitive content (where `sensitive: true`) must render with a visual indicator
  and respect the "hide sensitive" toggle state.

### 7.3 UI/UX Rules

- Morale flags must use consistent color coding: GREEN=green, YELLOW=amber, RED=red,
  UNKNOWN=gray.
- Markdown fields must render with a Markdown editor (not plain textarea).
- Overdue action items must be visually distinct (e.g., red badge/border).
- Empty states must be helpful (e.g., "No 1:1s yet — click 'Start 1:1' to begin").

---

## 8. Database & Migrations

- All schema changes MUST be done via **Flyway migrations**.
- Migration file naming: `V{timestamp}__{description}.sql`
  - Example: `V20250503120000__add_sensitive_flag_to_quick_notes.sql`
- Never edit existing migration files — always add a new one.
- Every migration must be tested with a Testcontainers-based integration test.
- Migrations must be idempotent where possible.
- After adding a migration, update `docs/DATA_MODEL.md` to reflect schema changes.

---

## 9. Security Checklist (Run Before Every Task Completion)

Before marking any backend task done, verify:

- [ ] All new endpoints require authentication
- [ ] All new queries are scoped by `userId`
- [ ] No sensitive data is logged (no PII, no note content in logs)
- [ ] Input validation is present on all request bodies (`@Valid` + Bean Validation)
- [ ] New endpoints have a security integration test
- [ ] `sensitive` flag is respected in API responses (no leaking flagged content
      in list views)
- [ ] Cross-manager access returns `404`, not `403` or `200`

---

## 10. Docker & Deployment

### 10.1 Services

```yaml
services:
  frontend:   # Next.js — port 3000
  api:        # Kotlin Spring Boot — port 8080
  db:         # PostgreSQL 16 — port 5432 (internal only)
```

### 10.2 Environment Variables

**API service:**

| Variable              | Description                          | Required |
|-----------------------|--------------------------------------|----------|
| `DB_URL`              | JDBC URL for PostgreSQL              | Yes      |
| `DB_USER`             | Database username                    | Yes      |
| `DB_PASSWORD`         | Database password                    | Yes      |
| `OIDC_ISSUER_URI`     | OIDC issuer (authentik URL)          | Yes      |
| `OIDC_JWKS_URI`       | JWKS endpoint URI                    | Yes      |
| `ENCRYPTION_KEY`      | Master key for sensitive field encryption | No* |

**Frontend service:**

| Variable              | Description                          | Required |
|-----------------------|--------------------------------------|----------|
| `NEXTAUTH_URL`        | Canonical URL of the frontend        | Yes      |
| `NEXTAUTH_SECRET`     | Auth.js session secret               | Yes      |
| `OIDC_CLIENT_ID`      | OIDC client ID from authentik        | Yes      |
| `OIDC_CLIENT_SECRET`  | OIDC client secret                   | Yes      |
| `OIDC_ISSUER`         | authentik OIDC issuer URL            | Yes      |
| `API_BASE_URL`        | Internal URL to backend API          | Yes      |

*`ENCRYPTION_KEY` is required if sensitive field encryption is enabled.

### 10.3 Rules for Docker Changes

- Any change to `docker-compose.yml` must be tested by running
  `docker compose up --build` and verifying all services start and health checks pass.
- New environment variables must be added to:
  - `docker-compose.yml` (with placeholder)
  - `README.md` (configuration table)
  - `.env.example`

---

## 11. Local Development (`dev.sh`)

It must **always** be possible to run the backend and frontend locally on the
developer's machine using the `./dev.sh` helper script:

```bash
# Start the backend (Kotlin Spring Boot) locally
./dev.sh backend

# Start the frontend (Next.js) locally
./dev.sh frontend
```

### Rules

- `./dev.sh backend` and `./dev.sh frontend` must work out of the box after
  cloning the repo and setting up `.env` (or `.env.local`).
- Any change to build configuration, dependencies, or environment variables
  must be validated against both `dev.sh` commands — **if either breaks, the
  task is not complete.**
- The script must handle installing dependencies, applying database migrations
  (backend), and starting the service with hot-reload enabled.
- `dev.sh` is the **primary** way developers run the project locally. Docker
  Compose is for production-like and CI environments.
- When adding new environment variables, ensure they are documented in
  `.env.example` and that `dev.sh` reads from the appropriate `.env` file.
- Agents must not remove or break `dev.sh` functionality. Treat a broken
  `dev.sh` the same as a broken build.

---

## 12. PROGRESS.md Specification

`PROGRESS.md` is the **session handoff document**. It must always be current.

### Required Structure:

```markdown
# PROGRESS.md

## Last Updated
{ISO 8601 timestamp} — {brief session description}

## Current Status
{One-paragraph summary of overall project state}

## Completed Features
- [x] {Feature name} — {brief description, date completed}
- [x] ...

## In Progress
- [ ] {Task name} — {what's done, what remains, any blockers}

## Known Issues / Bugs
| ID  | Description                     | Severity | Status   |
|-----|---------------------------------|----------|----------|
| 001 | {description}                   | High     | Open     |

## Next Steps (Prioritized)
1. {Next immediate task}
2. {Task after that}
3. ...

## Architecture Decisions Made This Session
- {Decision}: {Brief rationale} → see docs/ADR/{file}

## Environment / Setup Notes
{Anything a new agent needs to know to run the project locally}

## Test Coverage Summary
- Backend: {%} (last run: {date})
- Frontend: {%} (last run: {date})
- E2E: {passing}/{total} (last run: {date})

## Open Questions / Flags for Human Review
- {Question or ambiguity that needs a human decision}
```

---

## 13. README.md Update Checklist

When updating `README.md`, ensure the following sections exist and are current:

- [ ] Project description and purpose (1 paragraph)
- [ ] Prerequisites (Docker, Docker Compose versions, etc.)
- [ ] Quick start (clone → configure → `docker compose up`)
- [ ] Environment variable reference table
- [ ] Running tests (backend + frontend + e2e commands)
- [ ] authentik setup instructions (OIDC client creation)
- [ ] Backup and restore instructions (Postgres)
- [ ] Feature list (sync with completed items in PROGRESS.md)
- [ ] Contributing guidelines link or section
- [ ] License badge + statement

---

## 14. Architecture Decision Records (ADRs)

When making a non-trivial architectural decision:

1. Create `docs/ADR/YYYYMMDD-short-title.md`
2. Use this template:

```markdown
# ADR-{NNN}: {Title}

## Date
{YYYY-MM-DD}

## Status
Accepted | Superseded by ADR-{NNN}

## Context
{What situation or problem prompted this decision?}

## Decision
{What was decided?}

## Consequences
{What are the trade-offs, implications, follow-up actions?}
```

3. Reference the ADR in `PROGRESS.md` under "Architecture Decisions."

---

## 15. What Agents Must NOT Do

- ❌ Delete or modify existing Flyway migration files
- ❌ Remove test files or comment out failing tests to make a build pass
- ❌ Add `@Suppress` or `@SuppressWarnings` without a comment explaining why
- ❌ Hardcode credentials, secrets, or environment-specific values in code
- ❌ Implement HRIS features, compensation planning, or calibration
  (explicitly out of scope per PRD)
- ❌ Add Slack, Teams, Calendar, or Jira integrations (not in MVP)
- ❌ Make `Person` records shared between managers
- ❌ Skip `userId` scoping on any query
- ❌ Use H2 for tests (use Testcontainers + real PostgreSQL)
- ❌ Store tokens in browser `localStorage`
- ❌ Log note content, PII, or any user data at INFO level or above
- ❌ Mark a task complete without updating PROGRESS.md

---

## 16. Checklist — Task Completion Gate

Before declaring any task done, confirm **all** of the following:

```
TESTING
[ ] New tests written for all new code paths
[ ] All existing tests still pass
[ ] Security invariants tested (userId scoping, auth)
[ ] Coverage has not regressed

DOCUMENTATION  
[ ] README.md updated if feature/config changed
[ ] PROGRESS.md updated with current state
[ ] docs/ updated if architecture/model changed
[ ] New env vars added to .env.example

CODE QUALITY
[ ] No hardcoded secrets or credentials
[ ] No 'any' types in TypeScript without justification
[ ] Input validation present on all API endpoints
[ ] Error handling is consistent with API conventions

SECURITY
[ ] All new endpoints require authentication
[ ] All queries scoped by userId
[ ] No PII/note content in logs
[ ] Sensitive flag respected
```

---

*This file is the agent's constitution for this project.
Treat it as authoritative. When in doubt, re-read it.*
