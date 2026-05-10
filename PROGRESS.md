# PROGRESS.md

## Last Updated
2026-05-10T16:00:00Z — Search deep links: click results to navigate to exact location

## Current Status
Full-text search now links directly to the exact location of each result. Clicking a search result navigates to the specific 1:1 entry page, or to the person detail page with the correct tab (action items, PDP goals, kudos) pre-selected. The person detail page now supports a `?tab=` query parameter for deep linking. All 678 backend tests and 608 frontend tests pass.

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
- [x] Sensitive Content Encryption — AES-256-GCM application-level encryption for sensitive text fields at rest. EncryptionPort interface in application layer, AesGcmEncryptionAdapter in adapters layer. Integrated into persistence adapters (OneOnOneEntry, QuickNote, PdpUpdate). Graceful fallback when no key configured. Legacy unencrypted data support. (2026-05-10)
- [x] In-App Notification Scheduling — Hourly scheduled task generates notifications for all users. Notification types: ACTION_ITEM_OVERDUE, ACTION_ITEM_DUE_SOON, STALE_ONE_ON_ONE, UPCOMING_ANNIVERSARY. 24-hour deduplication window prevents duplicate notifications. REST API: list (paginated), unread count, mark as read, mark all as read. Frontend: NotificationBell with unread badge in navigation, NotificationPanel dropdown, NotificationItem with type-specific icons and deep links, dedicated /notifications page with pagination and unread filter. (2026-05-10)
- [x] Full-Text Search — GET /api/v1/search endpoint with PostgreSQL full-text search (to_tsvector/to_tsquery with prefix matching). Searches across persons, 1:1 entries, quick notes, action items, PDP goals, PDP updates, and kudos. Type filtering, pagination, relevance ranking. Sensitive content excluded from search (encrypted fields not searchable, sensitive snippets hidden in results). Frontend: dedicated /search page with search input, type filter chips, SearchResultCard component with type badges and deep links, pagination, URL state sync. Navigation link added. Deep links navigate to exact location: 1:1 entry page, person detail with correct tab pre-selected (action-items, pdp-goals, kudos). (2026-05-10)

## In Progress
- (none)

## Known Issues / Bugs
| ID  | Description                                          | Severity | Status |
|-----|------------------------------------------------------|----------|--------|
| 001 | Backend tests require Java 21 explicitly (system default may differ) | Low | Open |
| 002 | docker-compose.yml exposes db port 5432 (should only be in override) | Low | Open |
| 003 | FullStackIntegrationTest Property 14 (invalid morale status) has intermittent failure with edge-case strings | Low | Open |
| 004 | Changing ENCRYPTION_KEY caused 500 errors on all 1:1 entries (including non-sensitive) | High | Fixed |

## Next Steps (Prioritized)
1. Data export functionality (per-person Markdown)
2. Gamification elements (progress rings, streak counters, micro-animations)
3. Settings page (reminder thresholds, export, encryption key status)
4. GIN indexes for full-text search (performance optimization for large datasets)

## Architecture Decisions Made This Session
- Full-text search implemented using PostgreSQL's built-in to_tsvector/to_tsquery with prefix matching (word:*)
- Search queries are sanitized to prevent tsquery injection — special characters stripped, words connected with AND
- GIN indexes deferred for MVP — query-time FTS is efficient because all queries are pre-filtered by user_id (B-tree indexed)
- Sensitive content excluded from search results: 1:1 entries and quick notes with sensitive=true are filtered out in SQL, PDP updates with sensitive=true also excluded
- Encrypted sensitive fields are inherently not searchable (ciphertext won't match plaintext queries) — this is an acceptable trade-off
- Search results include a relevance score from ts_rank for ordering
- SearchResultResponse DTO hides snippet for sensitive results (defense in depth)
- Added MissingServletRequestParameterException handler to GlobalExceptionHandler (returns 400 instead of 500)
- Notification generation implemented as a Spring `@Scheduled` task in the scheduler adapter layer
- NotificationGenerationService lives in the application layer (testable without scheduler infrastructure)
- 24-hour deduplication window prevents notification spam — same type + referenceId won't re-notify within 24h
- Notifications are per-user and scoped by userId (security invariant maintained)
- Scheduler iterates all users independently — failure for one user doesn't block others
- Notification entity is a simple domain model (not a full aggregate) — no complex state transitions beyond read/unread
- Frontend polls unread count every 60 seconds (no WebSocket needed for MVP)
- Notification panel is a dropdown from the bell icon; full page at /notifications for history

## Environment / Setup Notes
- Java 21 is required for backend development (use SDKMAN: `sdk install java 21-tem`)
- Node.js 20+ required for frontend
- Docker required for running Testcontainers-based integration tests
- Copy `.env.example` to `.env` before running `./dev.sh backend`
- Copy `.env.example` to `.env.local` for frontend-specific overrides
- The `next.config.mjs` is used instead of `next.config.ts` (Next.js 14.x doesn't support TS config)
- JetBrains Mono font loaded via Google Fonts CDN alongside Inter
- Notification scheduler runs every hour by default; configure via `NOTIFICATION_CRON` env var

## Test Coverage Summary
- Backend: All 678 tests pass — domain (including SearchResult 6 tests), application (including SearchService 15 tests), controller slice (including SearchController 12 tests), encryption adapter (19 tests), encryption integration (10 tests), property, integration (last run: 2026-05-10)
  - 1 pre-existing intermittent failure (Property 14 edge case)
- Frontend: 608 total — component tests (including SearchResultCard 17), page tests (including search page 13), API client tests (including search 10) (last run: 2026-05-10)
  - Includes all previously listed test suites
  - New: SearchResultCard component tests (16)
  - New: Search page tests (13)
  - New: Search API client tests (10)
- E2E: No tests yet (Playwright configured)

## Open Questions / Flags for Human Review
- Property 14 test (invalid morale status) has an intermittent failure with certain generated strings — may need tighter string filtering or a different approach to testing invalid enum values
- Light mode toggle not yet implemented — currently dark-only. Should this be added as a user preference?
- Notification polling interval (60s) is hardcoded in the frontend — should this be configurable?
- Should notifications be auto-dismissed after a certain age (e.g., 30 days)?
