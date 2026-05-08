# PROGRESS.md

## Last Updated
2026-05-08T21:15:00Z — 1:1 Entry Management backend implemented (domain, application, persistence, web layers + tests)

## Current Status
The 1:1 Entry Management backend feature is fully implemented. The API provides endpoints for managing 1:1 series configuration (cadence + template) and 1:1 entries (CRUD with agenda items, Markdown notes, outcomes, and sensitive flag). The Person at-a-glance panel now includes the actual last 1:1 date computed from entry data. All new code has comprehensive test coverage across domain, application, and web layers. The frontend implementation for 1:1 entries is the next step.

## Completed Features
- [x] Backend project structure — Gradle Kotlin DSL, Spring Boot 3.3.5, Hexagonal/DDD package layout (2026-05-08)
- [x] Frontend project structure — Next.js 14, React 18, Auth.js, TypeScript strict mode (2026-05-08)
- [x] Backend Dockerfile — Multi-stage build (gradle:8-jdk21 → eclipse-temurin:21-jre-alpine) (2026-05-08)
- [x] Frontend Dockerfile — Multi-stage build (node:20-alpine, 3 stages with standalone output) (2026-05-08)
- [x] Docker Compose — Production stack with db, api, frontend services and health checks (2026-05-08)
- [x] Docker Compose override — Local dev with volume mounts and exposed ports (2026-05-08)
- [x] Local development script — dev.sh with backend/frontend commands, dependency checks (2026-05-08)
- [x] Environment documentation — .env.example with all variables documented (2026-05-08)
- [x] Person Directory API — Full CRUD, morale tracking, pinned remember items (2026-05-08)
- [x] OIDC Authentication — JWT validation, user provisioning, userId scoping (2026-05-08)
- [x] Database migrations — Users, persons, pinned_remember_items tables via Flyway (2026-05-08)
- [x] Backend tests — Domain unit tests, application service tests, controller slice tests, property tests, integration tests with Testcontainers (2026-05-08)
- [x] Frontend components — PersonCard, FilterBar, MoraleIndicator, Pagination, PersonForm, RememberItemsList, EmptyState (2026-05-08)
- [x] Frontend pages — People list, person detail, create person (2026-05-08)
- [x] Frontend tests — Component tests, page tests, API client tests (2026-05-08)
- [x] Frontend Auth.js integration — OIDC provider config, SessionProvider, middleware, sign-in page, route handler (2026-05-08)
- [x] README.md — Updated documentation reflecting current project state (2026-05-08)
- [x] 1:1 Entry Management Backend — Series config (cadence + template), entry CRUD, agenda items, sensitive flag, at-a-glance last 1:1 date (2026-05-08)
- [x] 1:1 Database migrations — one_on_one_series, one_on_one_entries, agenda_items tables (2026-05-08)
- [x] 1:1 Backend tests — Domain unit tests, application service tests, controller slice tests (2026-05-08)

## In Progress
- [ ] 1:1 Entry Management Frontend — TypeScript types, API client, components (timeline, entry editor, series config), pages

## Known Issues / Bugs
| ID  | Description                                          | Severity | Status |
|-----|------------------------------------------------------|----------|--------|
| 001 | Backend tests require Java 21 explicitly (system default may differ) | Low | Open |
| 002 | docker-compose.yml exposes db port 5432 (should only be in override) | Low | Open |
| 003 | FullStackIntegrationTest Property 14 (invalid morale status) has intermittent failure with edge-case strings | Low | Open |

## Next Steps (Prioritized)
1. 1:1 Entry Management Frontend (types, API client, components, pages)
2. Action Items (create, track, complete follow-ups from 1:1s)
3. PDP Goal tracking (personal development plans with status transitions)
4. Kudos recording (positive feedback and achievements)
5. Quick Notes ("Inbox") — global capture, attach to person/1:1
6. Dashboard (upcoming 1:1s, overdue items, stale 1:1 alerts)
7. Sensitive content encryption (flag and encrypt private notes)
8. Notification scheduling (reminders for overdue items and upcoming 1:1s)
9. Search (full-text across all manager data)
10. Data export functionality (per-person Markdown)

## Architecture Decisions Made This Session
- 1:1 entries nested under Person in URL structure: `/api/v1/persons/{personId}/one-on-one-entries/...` — enforces person-centric navigation
- Upsert semantics for series: PUT creates-or-updates since there's exactly one series per (user, person)
- Agenda items managed as embedded list within entry aggregate (full list replacement on update)
- Template prefill is server-side logic (applied during entry creation when notes are null)
- At-a-glance last1on1Date computed from actual entry data (not denormalized)

## Environment / Setup Notes
- Java 21 is required for backend development (use SDKMAN: `sdk install java 21-tem`)
- Node.js 20+ required for frontend
- Docker required for running Testcontainers-based integration tests
- Copy `.env.example` to `.env` before running `./dev.sh backend`
- Copy `.env.example` to `.env.local` for frontend-specific overrides
- The `next.config.mjs` is used instead of `next.config.ts` (Next.js 14.x doesn't support TS config)

## Test Coverage Summary
- Backend: 181 tests total — domain, application, controller slice, property, integration (last run: 2026-05-08)
  - 1 pre-existing intermittent failure (Property 14 edge case)
- Frontend: 7 component tests, 6 page tests (including auth pages), 1 API client test — 111 total (last run: 2026-05-08)
- E2E: No tests yet (Playwright configured)

## Open Questions / Flags for Human Review
- Property 14 test (invalid morale status) has an intermittent failure with certain generated strings — may need tighter string filtering or a different approach to testing invalid enum values
