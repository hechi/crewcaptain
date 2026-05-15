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
- **Action Items** — Create, track, complete, and cancel follow-ups from 1:1s with per-person and cross-person views, overdue filtering, owner type (manager/person), due dates, and status transitions (OPEN → DONE, OPEN → CANCELED). Full frontend with action items tab on person detail, status filter, inline create/edit forms, and cyberpunk-themed components. Inline action items section on the 1:1 entry page allows quick-adding action items during a session (auto-linked to the entry), viewing existing open items for the person, and marking items done — all without leaving the 1:1 page.
- **PDP Goal Tracking** — Personal development plans per person with goals (title, description, target date), status transitions (ACTIVE → ACHIEVED/PAUSED/DROPPED, PAUSED → ACTIVE), timestamped progress updates with sensitive flag, and full frontend with PDP goals tab, status filter, inline create/edit forms, and cyberpunk-themed components.
- **Kudos / Recognition** — Record positive feedback and achievements per person with date, Markdown text, and optional tags (e.g., "impact", "collaboration"). Full frontend with Kudos tab on person detail, inline create form, and delete. Immutable entries (create + delete only).
- **Quick Notes (Inbox)** — Global quick capture for thoughts, follow-ups, and observations. Notes can be unassigned (inbox), assigned to a person, or self-assigned (personal notes for the manager). Status workflow: INBOX → ATTACHED (to 1:1) / CONVERTED (to action item) / ARCHIVED. Supports sensitive flag. Self-assigned notes are accessible via "My Notes" in the user menu. Invariant: a note cannot be both self-assigned and assigned to a person — assigning to a person clears the self-assigned flag. Full frontend with dedicated Quick Notes page, My Notes page, status filter, and inline create form.
- **Dashboard** — At-a-glance overview showing overdue action items, due-soon items, stale 1:1 reminders (based on cadence), and upcoming work anniversaries. Configurable lookahead windows for due-soon (default 3 days) and anniversaries (default 30 days).
- **Sensitive Content Encryption** — Application-level AES-256-GCM encryption for sensitive text fields at rest. When `ENCRYPTION_KEY` is configured, all content marked `sensitive=true` (1:1 notes/outcomes, quick notes, PDP updates) is encrypted before storage and decrypted on read. Graceful fallback: without a key, the system operates normally with plaintext storage. Supports legacy unencrypted data migration (reads both encrypted and unencrypted content).
- **In-App Notifications** — Scheduled notification generation for overdue action items, due-soon items (configurable threshold, default 3 days), stale 1:1 reminders (based on cadence), and upcoming work anniversaries (7-day lookahead). Notification center with bell icon in navigation, unread badge, mark-as-read (individual and bulk), and dedicated notifications page with pagination and unread filter. Deduplication prevents duplicate notifications within 24 hours. Scheduler runs hourly by default (configurable via cron expression).
- **Full-Text Search** — Search across all manager data (people, 1:1 notes, quick notes, action items, PDP goals, kudos) using PostgreSQL full-text search with GIN indexes and relevance ranking. Type filters, pagination, and sensitive content protection (sensitive snippets hidden in results). Dedicated search page with real-time URL state and navigation link.
- **Per-Person Markdown Export** — Export all data for a person as a structured Markdown file: profile summary, pinned remember items, morale, 1:1 history (reverse chronological), action items (grouped by status), PDP goals with progress updates, and kudos. Optional date range filter. Sensitive content is marked but not exposed. Download via Export button on person detail page.
- **Gamification & Engagement** — Dashboard gamification elements for engagement: animated progress ring for PDP goal completion percentage, 1:1 streak counter (consecutive weeks with meetings), achievement badges for milestones (first 1:1, 10 action items closed, etc.), and activity heatmap (contribution-graph style). Micro-animation on task completion (checkmark with glow burst). All animations respect `prefers-reduced-motion`.
- **User Settings** — Per-user persistent settings page with: theme selection (dark/light), dashboard reminder thresholds (due-soon days, stale 1:1 days, anniversary lookahead), notification type toggles (overdue, due-soon, stale 1:1, anniversary), and achievement visibility toggle. Settings are stored in the database and respected by the notification scheduler and dashboard.
- **Light Theme** — Full light theme alternative to the default cyberpunk dark theme. Clean surfaces, teal/purple accents, proper contrast ratios, and subtle shadows instead of glows. Toggled via Settings page.
- **Review Packet Generator** — Generate structured review/performance summary documents for a person over a configurable date range. Includes executive summary with statistics (1:1 count, action item completion rate, PDP goal progress, kudos count), morale status, detailed 1:1 meeting history, action items grouped by status, PDP goals with progress updates, and kudos with tag summary. Sensitive content is excluded. Download as Markdown via "Review Packet" button on person detail page.
- **Bulk Import (CSV)** — Import multiple people at once from a CSV file. Supports columns: name (required), preferred_name, role_title, timezone, start_date (YYYY-MM-DD), email, tags (pipe-separated). Preview before import, per-row error reporting, max 500 rows per import. Accessible via "Import CSV" button on the People list page.
- **Soft-Delete + Restore** — Deleting a person moves them to trash (soft-delete) instead of permanently removing them. Trash page shows all deleted people with restore and permanent delete capability. All queries automatically exclude soft-deleted records. Data isolation enforced on trash operations. Permanent delete requires confirmation and cascades to all associated data (1:1 entries, action items, PDP goals, kudos).
- **Audit Log** — Records key actions (create, update, delete, restore) across all entities for the manager's own traceability. Paginated audit log page with entity type and action filters. All entries scoped by userId. Accessible via user menu in navigation.
- **Workspaces** — Lightweight organizational containers for grouping people (e.g., "My Team", "Mentees", "Skip-levels"). A workspace belongs to a single manager (private, no sharing). A person belongs to one workspace (optional). Opt-in: if no workspaces exist, everything works as before. Includes workspace CRUD, person-to-workspace assignment, workspace filter on People list, and management page accessible via user menu.
- **Landing Page** — Modern, high-converting landing page with cyberpunk-lite dark theme. Hero section with HUD visual motif, feature cards with glassmorphism, interactive screenshot showcase (tabbed gallery with Dashboard, Action Items, Person Detail, and Search views), 3-step deployment guide, privacy/self-hosted messaging, and dual CTA sections. Fully responsive, accessible (WCAG AA), respects `prefers-reduced-motion`. Authenticated users are redirected to the dashboard.
- **Prometheus Metrics** — Exposes application metrics at `/actuator/prometheus` for Prometheus scraping. Secured with a bearer token (`METRICS_TOKEN`). Includes JVM metrics, HTTP request metrics, HikariCP connection pool stats, and custom 1:1 metrics (total entries, entries in last 7 days). Health endpoint at `/actuator/health` remains unauthenticated for Docker healthchecks.

### Planned

- (none currently)

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

### Using Docker Compose (Production)

The production `docker-compose.yml` pulls pre-built images from the container registry:

```bash
# 1. Clone the repository
git clone https://github.com/your-org/crewcaptain.git
cd crewcaptain

# 2. Configure environment variables
cp .env.example .env
# Edit .env with your actual values (database credentials, OIDC settings)

# 3. Start the full stack (pulls images from registry)
docker compose up -d

# 4. Access the application
# Frontend: http://localhost:3000
# API:      http://localhost:8080
```

**Images:**
- `reg.root-base.de/poxy/crewcaptain/api:latest`
- `reg.root-base.de/poxy/crewcaptain/frontend:latest`

### Using Docker Compose (Local Development)

The `docker-compose.override.yml` adds build directives and dev tooling. When present, `docker compose up` will build from source:

```bash
# Copy the example override file
cp docker-compose.override.example.yml docker-compose.override.yml

# Build and start with local source (override is auto-loaded)
docker compose up --build
```

The override exposes the database port (5432), mounts source volumes for hot-reload, and sets development environment variables.

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
| DELETE | `/api/v1/persons/{id}`                  | Soft-delete a person (move to trash) |
| POST   | `/api/v1/persons/{id}/restore`          | Restore a soft-deleted person  |
| DELETE | `/api/v1/persons/{id}/permanent`        | Permanently delete a soft-deleted person |
| GET    | `/api/v1/persons/trash`                 | List deleted persons (paginated) |
| PUT    | `/api/v1/persons/{id}/morale`           | Set morale status              |
| POST   | `/api/v1/persons/{id}/remember-items`   | Add a pinned remember item     |
| DELETE | `/api/v1/persons/{id}/remember-items/{itemId}` | Remove a remember item  |
| PUT    | `/api/v1/persons/{id}/remember-items/reorder` | Reorder remember items  |
| GET    | `/api/v1/persons/{id}/export`           | Export person data as Markdown |
| GET    | `/api/v1/persons/{id}/review-packet`    | Generate review packet as Markdown |
| POST   | `/api/v1/persons/import`                | Bulk import persons from CSV   |

**Export query parameters:**
- `dateFrom` — Optional start date filter (ISO 8601 date, e.g., 2024-01-01)
- `dateTo` — Optional end date filter (ISO 8601 date, e.g., 2024-12-31)

**Export response:**
- Content-Type: `text/markdown; charset=UTF-8`
- Content-Disposition: `attachment; filename="export.md"`
- Body: Structured Markdown with profile, remember items, morale, 1:1 history, action items, PDP goals, and kudos

**Review packet query parameters (both required):**
- `dateFrom` — Start date of review period (ISO 8601 date, e.g., 2024-01-01)
- `dateTo` — End date of review period (ISO 8601 date, e.g., 2024-06-30)

**Review packet response:**
- Content-Type: `text/markdown; charset=UTF-8`
- Content-Disposition: `attachment; filename="review-packet.md"`
- Body: Structured Markdown with executive summary (statistics), morale, 1:1 meetings, action items (grouped by status with completion rate), PDP goals with progress, and kudos with tag summary. Sensitive content is excluded.

**Bulk import (POST `/api/v1/persons/import`):**
- Content-Type: `multipart/form-data`
- Form field: `file` — CSV file with header row
- Supported CSV columns: `name` (required), `preferred_name`, `role_title`, `timezone`, `start_date` (YYYY-MM-DD), `email`, `tags` (pipe-separated, e.g., "engineering|senior")
- Maximum 500 rows per import
- Response: `{ "successCount": 2, "errorCount": 1, "errors": ["Row 3: Name must not be blank"] }`
- Partial success: valid rows are imported even if some rows have errors

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
- `originatingEntryId` — Filter by originating 1:1 entry UUID (per-person endpoint only)
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
- Uses PostgreSQL full-text search with GIN indexes, prefix matching, and relevance ranking
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
- `selfAssigned` — Optional boolean (default: false). When true, the note is a personal note for the manager. Mutually exclusive with `personId`.

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
- `selfAssigned` — Filter by self-assigned flag (true/false)

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
| `METRICS_TOKEN`    | Bearer token for securing the `/actuator/prometheus` metrics endpoint. If not set, the endpoint returns 403. Generate with: `openssl rand -hex 32` | No |

### Frontend

| Variable            | Description                                     | Required |
|---------------------|-------------------------------------------------|----------|
| `NEXTAUTH_URL`      | Canonical URL of the frontend                   | Yes      |
| `NEXTAUTH_SECRET`   | Auth.js session secret                          | Yes      |
| `OIDC_CLIENT_ID`    | OIDC client ID from authentik                   | Yes      |
| `OIDC_CLIENT_SECRET`| OIDC client secret from authentik               | Yes      |
| `OIDC_ISSUER`       | authentik OIDC issuer URL                       | Yes      |
| `API_BASE_URL`      | Internal URL to backend API (read at runtime, e.g., `http://api:8080` for Docker or `https://api.example.com` for external) | Yes      |

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

## Monitoring (Prometheus + Grafana)

CrewCaptain exposes a Prometheus-compatible metrics endpoint for monitoring.

### Endpoints

| Endpoint | Auth | Description |
|----------|------|-------------|
| `/actuator/health` | None | Health check (used by Docker healthcheck) |
| `/actuator/prometheus` | Bearer token (`METRICS_TOKEN`) | Prometheus metrics scrape endpoint |

### Setup

1. Set `METRICS_TOKEN` in your `.env` file:
   ```bash
   METRICS_TOKEN=$(openssl rand -hex 32)
   ```

2. Configure your Prometheus `scrape_configs`:
   ```yaml
   scrape_configs:
     - job_name: 'crewcaptain-api'
       metrics_path: '/actuator/prometheus'
       bearer_token: '<your-METRICS_TOKEN-value>'
       static_configs:
         - targets: ['api:8080']
   ```

### Available Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `crewcaptain_one_on_ones` | Gauge | Total number of 1:1 entries across all users |
| `crewcaptain_one_on_ones_last_7_days` | Gauge | 1:1 entries with meeting date in the last 7 days |
| `jvm_memory_*` | Various | JVM memory usage (heap, non-heap, buffers) |
| `jvm_gc_*` | Various | Garbage collection stats |
| `hikaricp_*` | Various | Database connection pool metrics |
| `http_server_requests_*` | Timer | HTTP request latency and count by endpoint |
| `application_ready_time_seconds` | Gauge | Application startup time |

All metrics include the tag `application="crewcaptain"`.

### Security

- If `METRICS_TOKEN` is not configured, the `/actuator/prometheus` endpoint returns `403 Forbidden`.
- All other actuator endpoints (except `/actuator/health`) are blocked.
- The metrics endpoint does not expose any user data — only operational metrics.

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
| `V20250510120009` | Add GIN indexes for full-text search (immutable wrapper functions + expression indexes) |
| `V20250510120010` | Add soft-delete support to persons table (deleted_at column + indexes) |
| `V20250511120000` | Create audit_log table with indexes |
| `V20250511120001` | Cascade person delete to child tables (action_items, pdp_goals, kudos, quick_notes) |

New migrations must follow the naming convention: `V{timestamp}__{description}.sql`

---

## authentik Setup (OIDC)

1. In your authentik admin panel, create a new **OAuth2/OIDC Provider**:
   - Name: `crewcaptain`
   - Client type: Confidential
   - Redirect URIs: `http://localhost:3000/api/auth/callback/oidc`
   - Signing key: Select or create an RSA key
   - **Scopes**: Add the `offline_access` scope mapping (required for refresh tokens). If it doesn't exist, create a Property Mapping with scope name `offline_access` and expression `return {}`.

2. Create an **Application** linked to the provider:
   - Name: `CrewCaptain`
   - Slug: `crewcaptain`

3. Note the following values for your `.env`:
   - `OIDC_CLIENT_ID` — from the provider
   - `OIDC_CLIENT_SECRET` — from the provider
   - `OIDC_ISSUER_URI` / `OIDC_ISSUER` — `https://your-authentik/application/o/crewcaptain/`
   - `OIDC_JWKS_URI` — `https://your-authentik/application/o/crewcaptain/jwks/`

> **Note**: Without the `offline_access` scope mapping, authentik will not issue refresh tokens and users will be forced to re-login when the access token expires (typically every 5 minutes).

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

## CI/CD (GitLab)

The project includes a `.gitlab-ci.yml` pipeline for the GitLab instance at `git.root-base.de`.

### Pipeline Stages

| Stage | Trigger | Jobs |
|-------|---------|------|
| **test** | All branches | `test-api` (Gradle + Testcontainers), `test-frontend` (Jest) |
| **build** | `main` only | `build-api` (Docker image), `build-frontend` (Docker image) |

### Container Registry

Docker images are published to the GitLab Container Registry on every push to `main`:

- `<registry>/api:latest` and `<registry>/api:<commit-sha>`
- `<registry>/frontend:latest` and `<registry>/frontend:<commit-sha>`

### Runner Requirements

- GitLab runners must support Docker-in-Docker (`docker:27-dind` service)
- Docker-in-Docker is used for both Testcontainers (backend tests) and image builds

---

## Privacy & Telemetry

CrewCaptain is privacy-first. Next.js telemetry is **disabled** in the Docker image via the `NEXT_TELEMETRY_DISABLED=1` environment variable, set in both the build and runtime stages of the frontend Dockerfile. No anonymous usage data is sent to Vercel during builds or at runtime.

For local development outside Docker, you can disable telemetry manually:

```bash
npx next telemetry disable
```

Or set the environment variable in your shell:

```bash
export NEXT_TELEMETRY_DISABLED=1
```

See [Next.js Telemetry](https://nextjs.org/telemetry) for details on what would be collected if enabled.

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
