# PROGRESS.md

## Last Updated
2026-05-10T12:30:00Z — Dashboard feature (overdue items, due-soon items, stale 1:1 reminders, upcoming anniversaries)

## Current Status
The Dashboard feature is now complete. Authenticated users land on `/dashboard` which shows four sections: overdue action items, due-soon action items, stale 1:1 reminders (based on cadence), and upcoming work anniversaries. All data is scoped by userId. Backend has a dedicated DashboardService and DashboardController with configurable lookahead parameters. Frontend has four dashboard components with cyberpunk-lite styling, empty states, and person links. All 516 frontend tests and all backend tests pass.

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
- [x] Kudos / Recognition Backend API — Create, get, list (per-person + cross-person), delete. Date, Markdown text, optional tags. Data isolation enforced. (2026-05-10)
- [x] Kudos Database migration — kudos table with indexes (user_id, user_id+person_id, user_id+date) (2026-05-10)
- [x] Kudos Backend tests — Domain unit tests (8 tests), application service tests (10 tests), controller slice tests (12 tests), integration tests with data isolation verification (10 tests) (2026-05-10)
- [x] Kudos Frontend — TypeScript types, API client (5 functions), components (KudosCard, KudosForm, KudosList), person detail "Kudos" tab with inline create form and delete (2026-05-10)
- [x] Kudos Frontend tests — Component tests (KudosCard 8, KudosForm 9, KudosList 9), API client tests (14) (2026-05-10)
- [x] Quick Notes Backend API — Create, get, update, list, delete, assign-to-person, attach, convert, archive. Markdown text, optional person assignment, sensitive flag, status workflow (INBOX→ATTACHED/CONVERTED/ARCHIVED). Data isolation enforced. (2026-05-10)
- [x] Quick Notes Database migration — quick_notes table with indexes (user_id, user_id+status, user_id+person_id, user_id+created_at) (2026-05-10)
- [x] Quick Notes Backend tests — Domain unit tests (16 tests), application service tests (18 tests), controller slice tests (17 tests), integration tests with data isolation verification (15 tests) (2026-05-10)
- [x] Quick Notes Frontend — TypeScript types, API client (9 functions), components (QuickNoteCard with person picker + 1:1 entry picker, QuickNoteForm, QuickNoteList), dedicated Quick Notes page with status filter, pagination, person assignment, and 1:1 attachment, Navigation link (2026-05-10)
- [x] Quick Notes Frontend tests — Component tests (QuickNoteCard 16, QuickNoteForm 11, QuickNoteList 9), API client tests (17) (2026-05-10)
- [x] Quick Notes 1:1 Attachment — Backend schema migration (attached_entry_id FK), domain model updated, attach endpoint requires entryId, validates entry exists and belongs to user, adds note text as agenda item to the 1:1 entry, frontend entry picker UI (2026-05-10)
- [x] Quick Notes Action Item Conversion — Convert endpoint requires personId, creates actual action item with note text as title, assigns to person's action item list, frontend person picker for conversion (2026-05-10)
- [x] Dashboard Backend API — GET /api/v1/dashboard endpoint with configurable dueSoonDays and anniversaryLookaheadDays parameters. DashboardService aggregates overdue items, due-soon items, stale 1:1 reminders, and upcoming anniversaries. All queries scoped by userId. (2026-05-10)
- [x] Dashboard Backend tests — DashboardService unit tests (13 tests), DashboardController slice tests (8 tests) (2026-05-10)
- [x] Dashboard Frontend — TypeScript types, API client (getDashboard with options), components (OverdueActionItems, DueSoonActionItems, StaleOneOnOnes, UpcomingAnniversaries), dedicated Dashboard page with grid layout, alert summary, empty states, person links (2026-05-10)
- [x] Dashboard Frontend tests — Component tests (OverdueActionItems 8, DueSoonActionItems 8, StaleOneOnOnes 10, UpcomingAnniversaries 8), API client tests (8), page tests (10) (2026-05-10)
- [x] Navigation updated — Dashboard link added as first nav item, home page redirects to /dashboard (2026-05-10)

## In Progress
- (none)

## Known Issues / Bugs
| ID  | Description                                          | Severity | Status |
|-----|------------------------------------------------------|----------|--------|
| 001 | Backend tests require Java 21 explicitly (system default may differ) | Low | Open |
| 002 | docker-compose.yml exposes db port 5432 (should only be in override) | Low | Open |
| 003 | FullStackIntegrationTest Property 14 (invalid morale status) has intermittent failure with edge-case strings | Low | Open |

## Next Steps (Prioritized)
1. Sensitive content encryption (flag and encrypt private notes)
2. Notification scheduling (reminders for overdue items and upcoming 1:1s)
3. Search (full-text across all manager data)
4. Data export functionality (per-person Markdown)
5. Gamification elements (progress rings, streak counters, micro-animations)

## Architecture Decisions Made This Session
- Dashboard is a read-only aggregation endpoint — no new database tables needed
- Dashboard computes stale 1:1s by comparing last meeting date against cadence interval (WEEKLY=7d, BIWEEKLY=14d, MONTHLY=30d, CUSTOM=N days)
- If no meeting ever occurred for a series, staleness is computed from series creation date
- Overdue items limited to 10 (paginated at backend), due-soon items returned as full list
- Anniversary calculation handles year rollover (if anniversary already passed this year, shows next year's)
- Dashboard is the new landing page for authenticated users (replaces /people redirect)

## Environment / Setup Notes
- Java 21 is required for backend development (use SDKMAN: `sdk install java 21-tem`)
- Node.js 20+ required for frontend
- Docker required for running Testcontainers-based integration tests
- Copy `.env.example` to `.env` before running `./dev.sh backend`
- Copy `.env.example` to `.env.local` for frontend-specific overrides
- The `next.config.mjs` is used instead of `next.config.ts` (Next.js 14.x doesn't support TS config)
- JetBrains Mono font loaded via Google Fonts CDN alongside Inter

## Test Coverage Summary
- Backend: All tests pass — domain, application (including DashboardService 13 tests), controller slice (including DashboardController 8 tests), property, integration (last run: 2026-05-10)
  - 1 pre-existing intermittent failure (Property 14 edge case)
- Frontend: 516 total — component tests (including 4 dashboard components), page tests (including DashboardPage), API client tests (including dashboard) (last run: 2026-05-10)
  - Includes Dashboard components (OverdueActionItems 8, DueSoonActionItems 8, StaleOneOnOnes 10, UpcomingAnniversaries 8)
  - Includes Dashboard API client tests (8)
  - Includes Dashboard page tests (10)
  - Includes Navigation component tests (9 tests — added Dashboard link test)
  - Includes QuickNote components (QuickNoteCard 17, QuickNoteForm 11, QuickNoteList 9)
  - Includes QuickNote API client tests (17)
  - Includes Kudos components (KudosCard 8, KudosForm 9, KudosList 9)
  - Includes Kudos API client tests (14)
  - Includes PDP goal components (PdpGoalCard 18, PdpGoalForm 8, PdpGoalList 9, PdpGoalStatusBadge 4)
  - Includes PDP goal API client tests (19)
  - Includes PDP Goals tab integration tests (10)
  - Includes action item components (ActionItemCard 14, ActionItemForm 8, ActionItemList 9, ActionItemStatusBadge 3)
  - Includes action item API client tests (15)
  - Includes 1:1 components (timeline, entry editor, series config, Markdown editor, agenda items, sensitive toggle)
- E2E: No tests yet (Playwright configured)

## Open Questions / Flags for Human Review
- Property 14 test (invalid morale status) has an intermittent failure with certain generated strings — may need tighter string filtering or a different approach to testing invalid enum values
- Light mode toggle not yet implemented — currently dark-only. Should this be added as a user preference?
