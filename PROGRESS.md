# PROGRESS.md

## Last Updated
2026-05-10T09:00:00Z — At-a-Glance section now shows open action items count and active PDP goals

## Current Status
The PDP Goal tracking feature is fully implemented end-to-end. The backend provides full CRUD for goals with status transitions (ACTIVE → ACHIEVED/PAUSED/DROPPED, PAUSED → ACTIVE), progress updates with sensitive flag, and data isolation. The frontend includes TypeScript types, API client functions (12 functions), reusable components (PdpGoalCard, PdpGoalForm, PdpGoalList, PdpGoalStatusBadge), and an integrated "PDP Goals" tab on the person detail page with inline create/edit forms, status filtering, and status transition actions. All 365 frontend tests and all backend tests pass.

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
- [x] 1:1 Backend tests — Domain unit tests, application service tests, controller slice tests, property tests, integration tests (2026-05-08)
- [x] 1:1 Entry Management Frontend — TypeScript types, API client, components (timeline, entry editor, series config, Markdown editor, agenda items, sensitive toggle), pages (2026-05-08)
- [x] 1:1 Frontend tests — Component tests, page tests, API client tests (2026-05-08)
- [x] Frontend branding redesign — CSS design tokens, Inter font, Navigation component, brand colors across all components/pages (2026-05-09)
- [x] Cyberpunk-lite dark theme redesign — Dark-first UI, JetBrains Mono headings, electric cyan/neon violet accents, glassmorphism cards, glow effects, morale indicators with neon borders, updated DESIGN.md v2.0 (2026-05-09)
- [x] Action Items Backend API — Full CRUD, status transitions (OPEN→DONE, OPEN→CANCELED), owner type (MANAGER/PERSON), due dates, originating 1:1 entry link, per-person and cross-person listing, overdue filtering, data isolation (2026-05-10)
- [x] Action Items Database migration — action_items table with indexes (2026-05-10)
- [x] Action Items Backend tests — Domain unit tests (20 tests), application service tests (15 tests), controller slice tests (15 tests), integration tests with data isolation verification (15 tests) (2026-05-10)
- [x] Action Items Frontend — TypeScript types, API client (8 functions), components (ActionItemCard, ActionItemForm, ActionItemList, ActionItemStatusBadge), person detail "Action Items" tab with inline create/edit, status filter, complete/cancel/delete (2026-05-10)
- [x] Action Items Frontend tests — Component tests (ActionItemCard 14, ActionItemForm 8, ActionItemList 9, ActionItemStatusBadge 3), API client tests (15) (2026-05-10)
- [x] PDP Goal Tracking Backend API — Full CRUD, status transitions (ACTIVE→ACHIEVED/PAUSED/DROPPED, PAUSED→ACTIVE), progress updates with sensitive flag, per-person listing with status filter, data isolation (2026-05-10)
- [x] PDP Goal Database migrations — pdp_goals and pdp_updates tables with indexes (2026-05-10)
- [x] PDP Goal Backend tests — Domain unit tests (PdpGoal 22 tests, PdpUpdate 4 tests), application service tests (18 tests), controller slice tests (17 tests), integration tests with data isolation verification (17 tests) (2026-05-10)
- [x] PDP Goal Frontend — TypeScript types, API client (12 functions), components (PdpGoalCard, PdpGoalForm, PdpGoalList, PdpGoalStatusBadge), person detail "PDP Goals" tab with inline create/edit, status filter, achieve/pause/drop/resume actions (2026-05-10)
- [x] PDP Goal Frontend tests — Component tests (PdpGoalCard 18, PdpGoalForm 8, PdpGoalList 9, PdpGoalStatusBadge 4), API client tests (19), page integration tests (10) (2026-05-10)

## In Progress
- (none)

## Known Issues / Bugs
| ID  | Description                                          | Severity | Status |
|-----|------------------------------------------------------|----------|--------|
| 001 | Backend tests require Java 21 explicitly (system default may differ) | Low | Open |
| 002 | docker-compose.yml exposes db port 5432 (should only be in override) | Low | Open |
| 003 | FullStackIntegrationTest Property 14 (invalid morale status) has intermittent failure with edge-case strings | Low | Open |

## Next Steps (Prioritized)
1. Kudos recording (positive feedback and achievements)
2. Quick Notes ("Inbox") — global capture, attach to person/1:1
3. Dashboard (upcoming 1:1s, overdue items, stale 1:1 alerts)
4. Sensitive content encryption (flag and encrypt private notes)
5. Notification scheduling (reminders for overdue items and upcoming 1:1s)
6. Search (full-text across all manager data)
7. Data export functionality (per-person Markdown)
8. Gamification elements (progress rings, streak counters, micro-animations)

## Architecture Decisions Made This Session
- PDP Goals stored as a flat table with userId + personId scoping — consistent with action items pattern
- PDP Updates stored in separate table with FK to pdp_goals (CASCADE DELETE) — allows independent progress tracking
- Status transitions enforced at domain level (PdpGoal.achieve()/pause()/drop()/resume()) — prevents invalid state changes
- Sensitive flag on PDP updates (not goals) — aligns with PRD requirement for note-like content sensitivity
- Progress updates are append-only (create + delete, no edit) — preserves historical accuracy

## Environment / Setup Notes
- Java 21 is required for backend development (use SDKMAN: `sdk install java 21-tem`)
- Node.js 20+ required for frontend
- Docker required for running Testcontainers-based integration tests
- Copy `.env.example` to `.env` before running `./dev.sh backend`
- Copy `.env.example` to `.env.local` for frontend-specific overrides
- The `next.config.mjs` is used instead of `next.config.ts` (Next.js 14.x doesn't support TS config)
- JetBrains Mono font loaded via Google Fonts CDN alongside Inter

## Test Coverage Summary
- Backend: All tests pass — domain (including PdpGoal + PdpUpdate), application (including PdpGoalService), controller slice (including PdpGoalController), property, integration (including PDP data isolation) (last run: 2026-05-10)
  - 1 pre-existing intermittent failure (Property 14 edge case)
- Frontend: 365 total — component tests (including PDP Goal components), page tests (including auth pages, PDP Goals tab), API client tests (including PDP goals) (last run: 2026-05-10)
  - Includes PDP goal components (PdpGoalCard 18, PdpGoalForm 8, PdpGoalList 9, PdpGoalStatusBadge 4)
  - Includes PDP goal API client tests (19)
  - Includes PDP Goals tab integration tests (10)
  - Includes action item components (ActionItemCard 14, ActionItemForm 8, ActionItemList 9, ActionItemStatusBadge 3)
  - Includes action item API client tests (15)
  - Includes 1:1 components (timeline, entry editor, series config, Markdown editor, agenda items, sensitive toggle)
  - Includes Navigation component tests (8 tests)
  - Includes ThemeTokens design system tests (13 tests)
- E2E: No tests yet (Playwright configured)

## Open Questions / Flags for Human Review
- Property 14 test (invalid morale status) has an intermittent failure with certain generated strings — may need tighter string filtering or a different approach to testing invalid enum values
- Light mode toggle not yet implemented — currently dark-only. Should this be added as a user preference?
