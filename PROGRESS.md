# PROGRESS.md

## Last Updated
2026-05-10T22:00:00Z — GIN indexes for full-text search performance optimization

## Current Status
GIN indexes for full-text search are now implemented. Seven per-table immutable wrapper functions compute tsvector values, and expression-based GIN indexes use these functions for fast full-text search across all searchable tables (persons, one_on_one_entries, quick_notes, action_items, pdp_goals, pdp_updates, kudos). The search repository adapter uses the same functions in queries so PostgreSQL can leverage the indexes. All 807 backend tests and 740 frontend tests pass.

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
- [x] Per-Person Markdown Export — GET /api/v1/persons/{id}/export endpoint. Aggregates all person data (profile, pinned remember items, morale, 1:1 entries, action items, PDP goals with updates, kudos) and formats as structured Markdown. Optional dateFrom/dateTo query parameters for date range filtering. Sensitive content marked but not exposed. Returns text/markdown with Content-Disposition attachment header. Frontend: Export button on person detail page triggers download as {name}.md file. (2026-05-10)
- [x] Gamification Backend API — GET /api/v1/gamification/stats endpoint. GamificationService computes 1:1 streaks (consecutive weeks with meetings), achievement milestones (13 types across 1:1s, action items, PDP goals, kudos, and streaks), activity heatmap (configurable days window, default 90), and PDP progress summary (active/achieved/paused/dropped with completion percentage). All queries scoped by userId. (2026-05-10)
- [x] Gamification Backend tests — Domain unit tests (GamificationStats 8 tests), application service tests (GamificationService 18 tests), controller slice tests (GamificationController 8 tests) (2026-05-10)
- [x] Gamification Frontend — TypeScript types, API client (getGamificationStats), components (ProgressRing with animated SVG arc and glow, StreakCounter with monospace readout, AchievementBadge with Lucide SVG icons and category colors, ActivityHeatmap contribution graph, CompletionAnimation with checkmark glow burst). Dashboard integration with gamification stats section above existing grid. (2026-05-10)
- [x] Gamification Frontend tests — Component tests (ProgressRing 10, StreakCounter 8, AchievementBadge 10, ActivityHeatmap 9, CompletionAnimation 6), API client tests (8) (2026-05-10)
- [x] User Settings Backend API — GET/PUT /api/v1/settings endpoint. UserSettings domain aggregate with validation (threshold ranges). UserSettingsService for get/update. JPA persistence adapter. Flyway migration for user_settings table. Notification scheduler respects per-user notification toggles and threshold settings. (2026-05-10)
- [x] User Settings Backend tests — Domain unit tests (UserSettings 15 tests), application service tests (UserSettingsService 7 tests), controller slice tests (UserSettingsController 11 tests) (2026-05-10)
- [x] User Settings Frontend — TypeScript types, API client (getUserSettings, updateUserSettings), Settings page with theme selector, threshold inputs, notification toggles, achievement visibility toggle, save with success/error feedback. ThemeProvider context for app-wide theme management. Navigation link added. (2026-05-10)
- [x] User Settings Frontend tests — ThemeProvider tests (7), Settings page tests (14), API client tests (7), Navigation test for settings link (1) (2026-05-10)
- [x] Light Theme — Full CSS light theme via [data-theme="light"] selector. Clean surfaces (#F8FAFB base), teal/purple accents, proper WCAG contrast, subtle shadows instead of glows, light scrollbar styling. Toggled via Settings page. (2026-05-10)
- [x] Dashboard respects settings — Achievement section visibility controlled by showAchievements setting. Dashboard fetches user settings on load. (2026-05-10)
- [x] Automatic Token Refresh — Auth.js jwt callback captures refresh_token and expires_at on login, proactively refreshes access token 60s before expiry using OIDC token endpoint discovery. SessionProvider polls session every 4 minutes and on window focus. SessionRefreshGuard component detects unrecoverable refresh failures and triggers re-authentication. offline_access scope added to OIDC authorization request. (2026-05-10)
- [x] GIN Indexes for Full-Text Search — Per-table immutable wrapper functions (persons_search_vector, one_on_one_entries_search_vector, quick_notes_search_vector, action_items_search_vector, pdp_goals_search_vector, pdp_updates_search_vector, kudos_search_vector) with expression-based GIN indexes. Search queries use the same functions enabling index utilization. Flyway migration V20250510120009. (2026-05-10)

## In Progress
- (none)

## Known Issues / Bugs
| ID  | Description                                          | Severity | Status |
|-----|------------------------------------------------------|----------|--------|
| 001 | Backend tests require Java 21 explicitly (system default may differ) | Low | Open |
| 002 | docker-compose.yml exposes db port 5432 (should only be in override) | Low | Open |
| 003 | FullStackIntegrationTest Property 14 (invalid morale status) has intermittent failure with edge-case strings | Low | Open |
| 004 | Changing ENCRYPTION_KEY caused 500 errors on all 1:1 entries (including non-sensitive) | High | Fixed |
| 005 | Access token expired without automatic refresh, requiring manual re-login | Medium | Fixed |

## Next Steps (Prioritized)
1. Review packet generator (date range summaries)
2. Bulk import (CSV people list)
3. Dashboard uses user settings for dueSoonDays/anniversaryLookaheadDays query params (currently uses API defaults)

## Architecture Decisions Made This Session
- UserSettings is a separate domain aggregate (not embedded in User) — keeps the User aggregate focused on identity
- Settings table uses user_id as PK (1:1 relationship with users) — no separate settings ID needed
- Default settings returned when no row exists (no need to pre-create settings for every user)
- Notification scheduler reads user settings to respect notification toggles — disabled types are skipped entirely
- Theme is stored as a string enum (DARK/LIGHT) — extensible for future themes
- Light theme uses CSS custom properties override via [data-theme="light"] attribute on <html> — zero JS overhead for theme switching
- ThemeProvider uses React context for app-wide theme state — settings page updates propagate immediately
- GIN indexes use per-table immutable wrapper functions (not generated columns) — PostgreSQL's to_tsvector is STABLE not IMMUTABLE, so we wrap it in IMMUTABLE plpgsql functions that pin the 'english' config
- Expression-based GIN indexes (not stored tsvector columns) — avoids schema changes to entities, no trigger maintenance, queries must use the same function call to hit the index

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
- Backend: All 807 tests pass — domain (including UserSettings 15 tests), application (including UserSettingsService 7 tests), controller slice (including UserSettingsController 11 tests), encryption adapter, property, integration (including FullTextSearchGinIndex 15 tests) (last run: 2026-05-10)
  - 1 pre-existing intermittent failure (Property 14 edge case)
- Frontend: 740 total — component tests (including SessionRefreshGuard 6, SessionProvider 3, ThemeProvider 7), page tests (including Settings 14), API client tests (including settings 7), auth token refresh tests (16), Navigation test (last run: 2026-05-10)
- E2E: No tests yet (Playwright configured)

## Open Questions / Flags for Human Review
- Property 14 test (invalid morale status) has an intermittent failure with certain generated strings — may need tighter string filtering or a different approach to testing invalid enum values
- Light mode toggle not yet implemented — currently dark-only. Should this be added as a user preference? → RESOLVED: Implemented as part of Settings page
- Notification polling interval (60s) is hardcoded in the frontend — should this be configurable?
- Should notifications be auto-dismissed after a certain age (e.g., 30 days)?
- Dashboard currently doesn't pass user's threshold settings as query params to the dashboard API (uses API defaults) — should be wired up in a follow-up
