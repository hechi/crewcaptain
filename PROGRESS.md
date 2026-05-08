# PROGRESS.md

## Last Updated
2026-05-09T16:00:00Z — Branding refresh: updated DESIGN.md v2.0, new color palette, Plus Jakarta Sans heading font, motion tokens

## Current Status
The frontend has been refreshed with an updated brand identity (DESIGN.md v2.0). Key changes: refined color palette (deeper navy #162340, richer teal #1A9E8F, warm amber #E8763A), Plus Jakarta Sans as the heading font for differentiation, Inter retained for body text, new motion/transition tokens, hover elevation on cards, and semantic morale colors aligned with the design system. All 233 frontend tests pass and the build succeeds. The next priority is Action Items.

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
- [x] Branding refresh v2.0 — Plus Jakarta Sans heading font, refined color palette (deeper navy, richer teal, warm amber), motion/transition tokens, hover elevation, semantic morale colors (2026-05-09)

## In Progress
- (none)

## Known Issues / Bugs
| ID  | Description                                          | Severity | Status |
|-----|------------------------------------------------------|----------|--------|
| 001 | Backend tests require Java 21 explicitly (system default may differ) | Low | Open |
| 002 | docker-compose.yml exposes db port 5432 (should only be in override) | Low | Open |
| 003 | FullStackIntegrationTest Property 14 (invalid morale status) has intermittent failure with edge-case strings | Low | Open |

## Next Steps (Prioritized)
1. Action Items (create, track, complete follow-ups from 1:1s)
2. PDP Goal tracking (personal development plans with status transitions)
3. Kudos recording (positive feedback and achievements)
4. Quick Notes ("Inbox") — global capture, attach to person/1:1
5. Dashboard (upcoming 1:1s, overdue items, stale 1:1 alerts)
6. Sensitive content encryption (flag and encrypt private notes)
7. Notification scheduling (reminders for overdue items and upcoming 1:1s)
8. Search (full-text across all manager data)
9. Data export functionality (per-person Markdown)

## Architecture Decisions Made This Session
- DESIGN.md upgraded to v2.0 with refined branding based on 2026 SaaS trends research
- Plus Jakarta Sans chosen for headings over Montserrat/Source Sans Pro — more distinctive, geometric warmth, pairs well with Inter
- Color palette refined: deeper navy (#162340) for more premium feel, richer teal (#1A9E8F) less minty, warm amber (#E8763A) for better contrast
- Morale colors aligned with semantic colors (success green, warning gold, error red) for consistency
- Motion/transition tokens added as CSS custom properties (--transition-fast, --transition-normal, --transition-slow)
- Shadow system updated to use primary navy at low opacity for visual cohesion
- CSS custom properties (design tokens) chosen over CSS-in-JS or Tailwind for brand styling — keeps zero-dependency approach, works with inline styles, and provides a single source of truth for design tokens
- Navigation component renders only when authenticated — avoids showing nav on login/landing pages
- Fonts loaded via Google Fonts CDN in layout head — simple, performant, no build-time font processing needed
- Color system uses semantic variable names (--color-primary, --color-accent, etc.) mapped to DESIGN.md palette values

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
- Frontend: 233 total — component tests (including Navigation), page tests (including auth pages), API client tests (last run: 2026-05-09)
  - Includes 1:1 components (timeline, entry editor, series config, Markdown editor, agenda items, sensitive toggle)
  - Includes Navigation component tests (8 tests)
- E2E: No tests yet (Playwright configured)

## Open Questions / Flags for Human Review
- Property 14 test (invalid morale status) has an intermittent failure with certain generated strings — may need tighter string filtering or a different approach to testing invalid enum values
