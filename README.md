# CrewCaptain

[![License: AGPL-3.0](https://img.shields.io/badge/License-AGPL--3.0-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)

A self-hosted, privacy-first manager workspace for organizing people context, 1:1 history, development goals, action items, and kudos. CrewCaptain is not HR software — it's a private cockpit for people-centric leadership.

---

## Features

### Implemented

- **Person Directory** — Full CRUD for team members with name, preferred name, role title, timezone, start date, email, and tags
- **Morale Tracking** — Visual morale indicators (Green/Yellow/Red/Unknown) per person with optional notes
- **Pinned Remember Items** — Add, remove, and reorder quick-reference notes per person
- **Filtering & Pagination** — Filter people by tag or morale status with paginated results
- **At-a-Glance Summary** — Person detail includes last 1:1 date, open action items count, and active PDP goals (placeholder)
- **OIDC Authentication** — Secure login via authentik (OAuth2/OIDC) with automatic user provisioning
- **Data Isolation** — All queries scoped by authenticated user (manager) — no cross-user data access
- **Frontend UI** — People list, person detail, create person pages with filter bar, morale indicators, and empty states
- **Cyberpunk-Lite Design System** — Dark-first UI with electric cyan/neon violet accents, JetBrains Mono headings, glassmorphism cards, glow effects, Inter body text, CSS custom properties design tokens, and consistent navigation
- **1:1 Entry Management** — Full-stack series configuration (cadence + template), entry CRUD with agenda items, Markdown notes, outcomes, sensitive flag, paginated timeline, template prefill, and person at-a-glance last 1:1 date
- **Action Items** — Create, track, complete, and cancel follow-ups from 1:1s with per-person and cross-person views, overdue filtering, owner type (manager/person), due dates, and status transitions (OPEN → DONE, OPEN → CANCELED). Full frontend with action items tab on person detail, status filter, inline create/edit forms, and cyberpunk-themed components.
- **PDP Goal Tracking** — Personal development plans per person with goals (title, description, target date), status transitions (ACTIVE → ACHIEVED/PAUSED/DROPPED, PAUSED → ACTIVE), timestamped progress updates with sensitive flag, and full frontend with PDP goals tab, status filter, inline create/edit forms, and cyberpunk-themed components.
- **Kudos / Recognition** — Record positive feedback and achievements per person with date, Markdown text, and optional tags (e.g., "impact", "collaboration"). Full frontend with Kudos tab on person detail, inline create form, and delete. Immutable entries (create + delete only).
- **Quick Notes (Inbox)** — Global quick capture for thoughts, follow-ups, and observations. Notes can be unassigned (inbox) or assigned to a person. Status workflow: INBOX → ATTACHED (to 1:1) / CONVERTED (to action item) / ARCHIVED. Supports sensitive flag. Full frontend with dedicated Quick Notes page, status filter, and inline create form.
- **Dashboard** — At-a-glance overview showing overdue action items, due-soon items, stale 1:1 reminders (based on cadence), and upcoming work anniversaries. Configurable lookahead windows for due-soon (default 3 days) and anniversaries (default 30 days).
- **Sensitive Content Encryption** — Application-level AES-256-GCM encryption for sensitive text fields at rest. When `ENCRYPTION_KEY` is configured, all content marked `sensitive=true` (1:1 notes/outcomes, quick notes, PDP updates) is encrypted before storage and decrypted on read. Graceful fallback: without a key, the system operates normally with plaintext storage. Supports legacy unencrypted data migration (reads both encrypted and unencrypted content).
- **In-App Notifications** — Scheduled notification generation for overdue action items, due-soon items (configurable threshold, default 3 days), stale 1:1 reminders (based on cadence), and upcoming work anniversaries (7-day lookahead). Notification center with bell icon in navigation, unread badge, mark-as-read (individual and bulk), and dedicated notifications page with pagination and unread filter. Deduplication prevents duplicate notifications within 24 hours. Scheduler runs hourly by default (configurable via cron expression).
- **Full-Text Search** — Search across all manager data (people, 1:1 notes, quick notes, action items, PDP goals, kudos) using PostgreSQL full-text search with relevance ranking. Type filters, pagination, and sensitive content protection (sensitive snippets hidden in results). Dedicated search page with real-time URL state and navigation link.
- **Per-Person Markdown Export** — Export all data for a person as a structured Markdown file: profile summary, pinned remember items, morale, 1:1 history (reverse chronological), action items (grouped by status), PDP goals with progress updates, and kudos. Optional date range filter. Sensitive content is marked but not exposed. Download via Export button on person detail page.
- **Gamification & Engagement** — Dashboard gamification elements for engagement: animated progress ring for PDP goal completion percentage, 1:1 streak counter (consecutive weeks with meetings), achievement badges for milestones (first 1:1, 10 action items closed, etc.), and activity heatmap (contribution-graph style). Micro-animation on task completion (checkmark with glow burst). All animations respect `prefers-reduced-motion`.
- **User Settings** — Per-user persistent settings page with: theme selection (dark/light), dashboard reminder thresholds (due-soon days, stale 1:1 days, anniversary lookahead), notification type toggles (overdue, due-soon, stale 1:1, anniversary), and achievement visibility toggle. Settings are stored in the database and respected by the notification scheduler and dashboard.
- **Light Theme** — Full light theme alternative to the default cyberpunk dark theme. Clean surfaces, teal/purple accents, proper contrast ratios, and subtle shadows instead of glows. Toggled via Settings page.

### Planned

- GIN indexes for full-text search (performance optimization for large datasets)
- Review packet generator (date range summaries)
- Bulk import (CSV people list)

---

## Tech Stack

| Layer      | Technology                                    |
|------------|-----------------------------------------------|
| Backend    | Kotlin + Spring Boot 3.3.5 (Hexagonal/DDD)   |
| Frontend   | Next.js 14 + React 18 + Auth.js (OIDC)       |
| Database   | PostgreSQL 16                                 |
| Auth       | OAuth2 / OIDC via authentik                   |
| Deployment | Docker Compose                                |
| API Style  | REST + JSON                                   |
| Migrations | Flyway                                        |
| Testing    | JUnit 5 + Kotest + Testcontainers (backend), Jest + React Testing Library (frontend) |

---

## Prerequisites

- **Docker** 24+ and **Docker Compose** v2+
- **Java 21** (for local backend development)
- **Node.js 20+** and **npm** (for local frontend development)
- **authentik** instance (or any OIDC provider) for authentication

---

## Quick Start

### Using Docker Compose (recommended)

```bash
# 1. Clone the repository
git clone https://github.com/your-org/crewcaptain.git
cd crewcaptain

# 2. Configure environment variables
cp .env.example .env
# Edit .env with your actual values (database credentials, OIDC settings)

# 3. Start the full stack
docker compose up --build

# 4. Access the application
# Frontend: http://localhost:3000
# API:      http://localhost:8080
```

### Local Development

CrewCaptain uses `dev.sh` as the primary local development runner:

```bash
# Start the backend (requires Java 21, PostgreSQL running)
./dev.sh backend

# Start the frontend (requires Node.js 20+)
./dev.sh frontend
```

The script handles dependency installation, database migrations (Flyway), and starts services with hot-reload enabled.

**Backend** runs on `http://localhost:8080`
**Frontend** runs on `http://localhost:3000`

---

## API Endpoints

All endpoints require `Authorization: Bearer <jwt>` header. Base path: `/api/v1/`

### Person Directory

| Method | Endpoint                                | Description                    |
|--------|-----------------------------------------|--------------------------------|
| POST   | `/api/v1/persons`                       | Create a new person            |
| GET    | `/api/v1/persons`                       | List persons (paginated)       |
| GET    | `/api/v1/persons/{id}`                  | Get person by ID               |
| PUT    | `/api/v1/persons/{id}`                  | Update a person                |
| DELETE | `/api/v1/persons/{id}`                  | Delete a person                |
| PUT    | `/api/v1/persons/{id}/morale`           | Set morale status              |
| POST   | `/api/v1/persons/{id}/remember-items`   | Add a pinned remember item     |
| DELETE | `/api/v1/persons/{id}/remember-items/{itemId}` | Remove a remember item  |
| PUT    | `/api/v1/persons/{id}/remember-items/reorder` | Reorder remember items  |
| GET    | `/api/v1/persons/{id}/export`           | Export person data as Markdown |

**Export query parameters:**
- `dateFrom` — Optional start date filter (ISO 8601 date, e.g., 2024-01-01)
- `dateTo` — Optional end date filter (ISO 8601 date, e.g., 2024-12-31)

**Export response:**
- Content-Type: `text/markdown; charset=UTF-8`
- Content-Disposition: `attachment; filename="export.md"`
- Body: Structured Markdown with profile, remember items, morale, 1:1 history, action items, PDP goals, and kudos

### 1:1 Entry Management

| Method | Endpoint                                                    | Description                        |
|--------|-------------------------------------------------------------|------------------------------------|
| PUT    | `/api/v1/persons/{personId}/one-on-one-series`              | Create/update 1:1 series config    |
| GET    | `/api/v1/persons/{personId}/one-on-one-series`              | Get 1:1 series config              |
| POST   | `/api/v1/persons/{personId}/one-on-one-entries`             | Create a 1:1 entry                 |
| GET    | `/api/v1/persons/{personId}/one-on-one-entries`             | List 1:1 entries (paginated)       |
| GET    | `/api/v1/persons/{personId}/one-on-one-entries/{entryId}`   | Get a 1:1 entry                    |
| PUT    | `/api/v1/persons/{personId}/one-on-one-entries/{entryId}`   | Update a 1:1 entry                 |
| DELETE | `/api/v1/persons/{personId}/one-on-one-entries/{entryId}`   | Delete a 1:1 entry                 |

### Action Items

| Method | Endpoint                                                              | Description                          |
|--------|-----------------------------------------------------------------------|--------------------------------------|
| POST   | `/api/v1/persons/{personId}/action-items`                             | Create an action item                |
| GET    | `/api/v1/persons/{personId}/action-items`                             | List action items for a person       |
| GET    | `/api/v1/persons/{personId}/action-items/{actionItemId}`              | Get an action item                   |
| PUT    | `/api/v1/persons/{personId}/action-items/{actionItemId}`              | Update an action item                |
| POST   | `/api/v1/persons/{personId}/action-items/{actionItemId}/complete`     | Mark action item as DONE             |
| POST   | `/api/v1/persons/{personId}/action-items/{actionItemId}/cancel`       | Mark action item as CANCELED         |
| DELETE | `/api/v1/persons/{personId}/action-items/{actionItemId}`              | Delete an action item                |
| GET    | `/api/v1/action-items`                                                | List all action items (cross-person) |

**Action Item fields:**
- `title` — Required (max 500 chars)
- `description` — Optional text
- `ownerType` — MANAGER or PERSON (default: MANAGER)
- `dueDate` — Optional date (ISO 8601 date, e.g., 2026-05-20)
- `originatingEntryId` — Optional UUID linking to a 1:1 entry

**Query parameters for action items list endpoints:**
- `page` — Page number (default: 0)
- `size` — Page size (default: 20)
- `status` — Filter by status (OPEN, DONE, CANCELED)
- `overdueOnly` — Only return overdue items (cross-person endpoint only, default: false)

**Status transitions:**
- OPEN → DONE (via `/complete`)
- OPEN → CANCELED (via `/cancel`)
- No other transitions are allowed

### PDP Goals (Personal Development Plans)

| Method | Endpoint                                                              | Description                          |
|--------|-----------------------------------------------------------------------|--------------------------------------|
| POST   | `/api/v1/persons/{personId}/pdp-goals`                                | Create a PDP goal                    |
| GET    | `/api/v1/persons/{personId}/pdp-goals`                                | List PDP goals for a person          |
| GET    | `/api/v1/persons/{personId}/pdp-goals/{goalId}`                       | Get a PDP goal                       |
| PUT    | `/api/v1/persons/{personId}/pdp-goals/{goalId}`                       | Update a PDP goal                    |
| POST   | `/api/v1/persons/{personId}/pdp-goals/{goalId}/achieve`               | Mark goal as ACHIEVED                |
| POST   | `/api/v1/persons/{personId}/pdp-goals/{goalId}/pause`                 | Mark goal as PAUSED                  |
| POST   | `/api/v1/persons/{personId}/pdp-goals/{goalId}/drop`                  | Mark goal as DROPPED                 |
| POST   | `/api/v1/persons/{personId}/pdp-goals/{goalId}/resume`                | Resume a PAUSED goal to ACTIVE       |
| DELETE | `/api/v1/persons/{personId}/pdp-goals/{goalId}`                       | Delete a PDP goal                    |
| POST   | `/api/v1/persons/{personId}/pdp-goals/{goalId}/updates`               | Add a progress update                |
| GET    | `/api/v1/persons/{personId}/pdp-goals/{goalId}/updates`               | List progress updates                |
| DELETE | `/api/v1/persons/{personId}/pdp-goals/{goalId}/updates/{updateId}`    | Delete a progress update             |

**PDP Goal fields:**
- `title` — Required (max 500 chars)
- `description` — Optional text
- `targetDate` — Optional date (ISO 8601 date)

**PDP Goal status transitions:**
- ACTIVE → ACHIEVED (via `/achieve`)
- ACTIVE → PAUSED (via `/pause`)
- ACTIVE → DROPPED (via `/drop`)
- PAUSED → ACTIVE (via `/resume`)
- No other transitions are allowed

**PDP Update fields:**
- `textMarkdown` — Required (Markdown text)
- `sensitive` — Optional boolean (default: false)

**Query parameters for PDP goals list endpoint:**
- `page` — Page number (default: 0)
- `size` — Page size (default: 20)
- `status` — Filter by status (ACTIVE, ACHIEVED, PAUSED, DROPPED)

### Kudos / Recognition

| Method | Endpoint                                                | Description                          |
|--------|---------------------------------------------------------|--------------------------------------|
| POST   | `/api/v1/persons/{personId}/kudos`                      | Create a kudos entry                 |
| GET    | `/api/v1/persons/{personId}/kudos`                      | List kudos for a person (paginated)  |
| GET    | `/api/v1/persons/{personId}/kudos/{kudosId}`            | Get a kudos entry                    |
| DELETE | `/api/v1/persons/{personId}/kudos/{kudosId}`            | Delete a kudos entry                 |
| GET    | `/api/v1/kudos`                                         | List all kudos (cross-person)        |

**Kudos fields:**
- `text` — Required (Markdown text)
- `date` — Optional date (ISO 8601 date, defaults to today)
- `tags` — Optional list of strings (e.g., ["impact", "collaboration"])

**Query parameters for kudos list endpoints:**
- `page` — Page number (default: 0)
- `size` — Page size (default: 20)

### Quick Notes (Inbox)

| Method | Endpoint                                                | Description                          |
|--------|---------------------------------------------------------|--------------------------------------|
| POST   | `/api/v1/quick-notes`                                   | Create a quick note                  |
| GET    | `/api/v1/quick-notes`                                   | List quick notes (paginated)         |
| GET    | `/api/v1/quick-notes/{quickNoteId}`                     | Get a quick note                     |
| PUT    | `/api/v1/quick-notes/{quickNoteId}`                     | Update a quick note                  |
| DELETE | `/api/v1/quick-notes/{quickNoteId}`                     | Delete a quick note                  |
| POST   | `/api/v1/quick-notes/{quickNoteId}/assign`              | Assign to a person                   |
| POST   | `/api/v1/quick-notes/{quickNoteId}/attach`              | Attach to a 1:1 entry              |
| POST   | `/api/v1/quick-notes/{quickNoteId}/convert`             | Mark as converted (to action item)   |
| POST   | `/api/v1/quick-notes/{quickNoteId}/archive`             | Archive the quick note               |

### Dashboard

| Method | Endpoint                | Description                                    |
|--------|-------------------------|------------------------------------------------|
| GET    | `/api/v1/dashboard`     | Get dashboard data (overdue, due-soon, stale 1:1s, anniversaries) |

**Query parameters:**
- `dueSoonDays` — Number of days to look ahead for due-soon items (default: 3)
- `anniversaryLookaheadDays` — Number of days to look ahead for anniversaries (default: 30)

### Notifications

| Method | Endpoint                                    | Description                          |
|--------|---------------------------------------------|--------------------------------------|
| GET    | `/api/v1/notifications`                     | List notifications (paginated)       |
| GET    | `/api/v1/notifications/unread-count`        | Get unread notification count        |
| POST   | `/api/v1/notifications/{notificationId}/read` | Mark a notification as read        |
| POST   | `/api/v1/notifications/read-all`            | Mark all notifications as read       |

### Search

| Method | Endpoint          | Description                                    |
|--------|-------------------|------------------------------------------------|
| GET    | `/api/v1/search`  | Full-text search across all manager data       |

### User Settings

| Method | Endpoint          | Description                                    |
|--------|-------------------|------------------------------------------------|
| GET    | `/api/v1/settings` | Get current user settings (returns defaults if none saved) |
| PUT    | `/api/v1/settings` | Update user settings                          |

**Settings fields:**
- `dueSoonDays` — Days before due date to show "due soon" (1–30, default: 3)
- `staleOneOnOneDays` — Days without a 1:1 before it's considered stale (1–90, default: 14)
- `anniversaryLookaheadDays` — Days to look ahead for anniversaries (1–90, default: 30)
- `theme` — UI theme: `DARK` or `LIGHT` (default: DARK)
- `showAchievements` — Show achievement badges on dashboard (default: true)
- `notifyActionItemOverdue` — Enable overdue action item notifications (default: true)
- `notifyActionItemDueSoon` — Enable due-soon action item notifications (default: true)
- `notifyStaleOneOnOne` — Enable stale 1:1 notifications (default: true)
- `notifyUpcomingAnniversary` — Enable anniversary notifications (default: true)

**Query parameters:**
- `q` — Search query (required)
- `type` — Filter by result type (repeatable): PERSON, ONE_ON_ONE_ENTRY, QUICK_NOTE, ACTION_ITEM, PDP_GOAL, PDP_UPDATE, KUDOS
- `page` — Page number (default: 0)
- `size` — Page size (default: 20, max: 100)

**Response fields:**
- `results` — Array of search results with id, type, title, snippet, personId, personName, sensitive, createdAt, relevanceScore
- `query` — The original search query
- `totalCount` — Total number of matching results
- `page` / `size` / `totalPages` — Pagination metadata

**Notes:**
- All results are scoped by the authenticated user (security invariant)
- Sensitive content snippets are hidden in search results (only title shown)
- Uses PostgreSQL full-text search with prefix matching and relevance ranking
- Encrypted sensitive fields are not searchable (trade-off for encryption at rest)

**Query parameters for notifications list:**
- `page` — Page number (default: 0)
- `size` — Page size (default: 20)
- `unreadOnly` — Only return unread notifications (default: false)

**Notification types:**
- `ACTION_ITEM_OVERDUE` — Action item past its due date
- `ACTION_ITEM_DUE_SOON` — Action item due within the configured threshold
- `STALE_ONE_ON_ONE` — 1:1 meeting overdue based on cadence
- `UPCOMING_ANNIVERSARY` — Work anniversary approaching

**Quick Note fields:**
- `text` — Required (Markdown text)
- `personId` — Optional UUID to assign to a person
- `sensitive` — Optional boolean (default: false)

**Quick Note status transitions:**
- INBOX → ATTACHED (via `/attach` with `entryId` — links to a specific 1:1 entry)
- INBOX → CONVERTED (via `/convert`)
- INBOX → ARCHIVED (via `/archive`)
- No other transitions are allowed

**Query parameters for quick notes list endpoint:**
- `page` — Page number (default: 0)
- `size` — Page size (default: 20)
- `status` — Filter by status (INBOX, ATTACHED, CONVERTED, ARCHIVED)
- `personId` — Filter by assigned person

**Query parameters for entries list endpoint:**
- `page` — Page number (default: 0)
- `size` — Page size (default: 20)

**1:1 Series fields:**
- `cadenceType` — WEEKLY, BIWEEKLY, MONTHLY, or CUSTOM
- `customIntervalDays` — Required when cadenceType is CUSTOM (positive integer)
- `templateMarkdown` — Markdown template to prefill new entries

**1:1 Entry fields:**
- `meetingDate` — Required (ISO 8601 timestamp)
- `agendaItems` — List of `{ text, checked }` objects
- `notesMarkdown` — Markdown notes (prefilled from template if not provided)
- `outcomesMarkdown` — Markdown outcomes/decisions
- `sensitive` — Boolean flag for sensitive content (default: false)

**Query parameters for list endpoint:**
- `page` — Page number (default: 0)
- `size` — Page size (default: 20)
- `tag` — Filter by tag
- `morale` — Filter by morale status (GREEN, YELLOW, RED, UNKNOWN)

---

## Environment Variables

### Backend (API)

| Variable           | Description                                      | Required |
|--------------------|--------------------------------------------------|----------|
| `DB_URL`           | JDBC URL for PostgreSQL                          | Yes      |
| `DB_USER`          | Database username                                | Yes      |
| `DB_PASSWORD`      | Database password                                | Yes      |
| `OIDC_ISSUER_URI`  | OIDC issuer URI (authentik provider URL)         | Yes      |
| `OIDC_JWKS_URI`    | JWKS endpoint URI                                | Yes      |
| `ENCRYPTION_KEY`   | Base64-encoded 32-byte AES key for sensitive field encryption (generate with: `openssl rand -base64 32`) | No*      |
| `NOTIFICATION_CRON` | Cron expression for notification scheduler (default: `0 0 * * * *` — every hour) | No |
| `NOTIFICATION_DUE_SOON_DAYS` | Days before due date to trigger "due soon" notifications (default: 3) | No |
| `NOTIFICATION_ANNIVERSARY_LOOKAHEAD_DAYS` | Days to look ahead for anniversary notifications (default: 7) | No |

### Frontend

| Variable            | Description                                     | Required |
|---------------------|-------------------------------------------------|----------|
| `NEXTAUTH_URL`      | Canonical URL of the frontend                   | Yes      |
| `NEXTAUTH_SECRET`   | Auth.js session secret                          | Yes      |
| `OIDC_CLIENT_ID`    | OIDC client ID from authentik                   | Yes      |
| `OIDC_CLIENT_SECRET`| OIDC client secret from authentik               | Yes      |
| `OIDC_ISSUER`       | authentik OIDC issuer URL                       | Yes      |
| `API_BASE_URL`      | Internal URL to backend API                     | Yes      |

*`ENCRYPTION_KEY` is required when sensitive field encryption is enabled. Generate with: `openssl rand -base64 32`. Must be exactly 32 bytes when Base64-decoded (256-bit AES key). Without this key, sensitive content is stored in plaintext (the `sensitive` flag still works for UI labeling).

See `.env.example` for a complete template with placeholder values.

---

## Running Tests

```bash
# Backend — all tests (requires Docker for Testcontainers)
cd api && ./gradlew test

# Backend — specific layer
./gradlew test --tests "com.peoplemanager.domain.*"
./gradlew test --tests "com.peoplemanager.application.*"
./gradlew test --tests "com.peoplemanager.adapters.web.*"
./gradlew test --tests "com.peoplemanager.integration.*"

# Frontend — unit + component tests
cd frontend && npm test

# Frontend — tests with coverage
npm run test:coverage

# Frontend — end-to-end tests (requires running stack)
npm run test:e2e

# dev.sh tests
./tests/test_dev_sh.sh
```

All backend database tests use Testcontainers with real PostgreSQL — no H2.

---

## Database Migrations

Schema changes are managed via Flyway. Current migrations:

| Migration | Description |
|-----------|-------------|
| `V20250508120000` | Create users table |
| `V20250508120001` | Create persons table |
| `V20250508120002` | Create pinned_remember_items table |
| `V20250508120003` | Create one_on_one_series table |
| `V20250508120004` | Create one_on_one_entries table |
| `V20250508120005` | Create agenda_items table |
| `V20250510120000` | Create action_items table |
| `V20250510120001` | Create pdp_goals table |
| `V20250510120002` | Create pdp_updates table |
| `V20250510120003` | Create kudos table |
| `V20250510120004` | Create quick_notes table |
| `V20250510120005` | Add attached_entry_id to quick_notes |
| `V20250510120006` | Create notifications table |
| `V20250510120007` | Add full-text search support (placeholder — no schema changes needed) |
| `V20250510120008` | Create user_settings table |

New migrations must follow the naming convention: `V{timestamp}__{description}.sql`

---

## authentik Setup (OIDC)

1. In your authentik admin panel, create a new **OAuth2/OIDC Provider**:
   - Name: `crewcaptain`
   - Client type: Confidential
   - Redirect URIs: `http://localhost:3000/api/auth/callback/oidc`
   - Signing key: Select or create an RSA key

2. Create an **Application** linked to the provider:
   - Name: `CrewCaptain`
   - Slug: `crewcaptain`

3. Note the following values for your `.env`:
   - `OIDC_CLIENT_ID` — from the provider
   - `OIDC_CLIENT_SECRET` — from the provider
   - `OIDC_ISSUER_URI` / `OIDC_ISSUER` — `https://your-authentik/application/o/crewcaptain/`
   - `OIDC_JWKS_URI` — `https://your-authentik/application/o/crewcaptain/jwks/`

---

## Backup and Restore

### Backup PostgreSQL

```bash
# Using Docker Compose
docker compose exec db pg_dump -U crewcaptain crewcaptain > backup_$(date +%Y%m%d).sql

# Direct connection
pg_dump -h localhost -U crewcaptain -d crewcaptain > backup_$(date +%Y%m%d).sql
```

### Restore PostgreSQL

```bash
# Using Docker Compose
cat backup_20250508.sql | docker compose exec -T db psql -U crewcaptain -d crewcaptain

# Direct connection
psql -h localhost -U crewcaptain -d crewcaptain < backup_20250508.sql
```

---

## Project Structure

```
/
├── dev.sh                     ← Local dev runner (./dev.sh backend | frontend)
├── docker-compose.yml         ← Production-like stack
├── .env.example               ← Environment variable template
│
├── api/                       ← Kotlin Spring Boot backend
│   ├── src/main/kotlin/com/peoplemanager/
│   │   ├── domain/            ← Aggregates, Value Objects (Person, User, PinnedRememberItem)
│   │   ├── application/       ← Use Cases, Ports, Commands, Queries
│   │   └── adapters/
│   │       ├── web/           ← REST Controllers + DTOs
│   │       ├── persistence/   ← JPA Repositories + Entities
│   │       ├── auth/          ← OIDC/JWT verification + user provisioning
│   │       └── scheduler/     ← Notification generation (hourly scheduled)
│   ├── src/main/resources/db/migration/ ← Flyway migrations
│   ├── src/test/kotlin/       ← Tests (domain, application, web, integration)
│   ├── build.gradle.kts
│   └── Dockerfile
│
└── frontend/                  ← Next.js frontend
    ├── src/
    │   ├── app/               ← App Router pages (people list, detail, create)
    │   ├── components/        ← UI components (PersonCard, FilterBar, MoraleIndicator, etc.)
    │   ├── lib/               ← API client
    │   └── types/             ← TypeScript type definitions
    ├── __tests__/             ← Jest + React Testing Library (components, lib, pages)
    ├── e2e/                   ← Playwright end-to-end tests (planned)
    ├── package.json
    └── Dockerfile
```

---

## Contributing

1. Read `AGENTS.md` for the full development workflow and architecture rules
2. Follow the mandatory workflow: Branch → Read → Plan → Test → Code → Verify → Docs → Log → Commit
3. Every change must include tests — no exceptions
4. All data queries must be scoped by `userId` (security invariant)
5. Update `README.md` and `PROGRESS.md` with every change
6. Use conventional commits (see `AGENTS.md` §4.3)

---

## License

This project is licensed under the [GNU Affero General Public License v3.0](https://www.gnu.org/licenses/agpl-3.0.html).
