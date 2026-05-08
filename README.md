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
- **Brand Design System** — CrewCaptain branding with deep navy/teal color palette, Inter typography, CSS custom properties design tokens, and consistent navigation
- **1:1 Entry Management** — Full-stack series configuration (cadence + template), entry CRUD with agenda items, Markdown notes, outcomes, sensitive flag, paginated timeline, template prefill, and person at-a-glance last 1:1 date

### Planned

- **PDP Tracking** — Track personal development goals with status transitions
- **Action Items** — Create, assign, and track follow-ups from meetings
- **Kudos** — Record positive feedback and achievements
- **Sensitive Content** — Flag and hide sensitive notes with encryption support
- **Notifications** — Scheduled reminders for overdue items and upcoming 1:1s
- **Data Export** — Full data export capabilities

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
| `ENCRYPTION_KEY`   | Master key for sensitive field encryption        | No*      |

### Frontend

| Variable            | Description                                     | Required |
|---------------------|-------------------------------------------------|----------|
| `NEXTAUTH_URL`      | Canonical URL of the frontend                   | Yes      |
| `NEXTAUTH_SECRET`   | Auth.js session secret                          | Yes      |
| `OIDC_CLIENT_ID`    | OIDC client ID from authentik                   | Yes      |
| `OIDC_CLIENT_SECRET`| OIDC client secret from authentik               | Yes      |
| `OIDC_ISSUER`       | authentik OIDC issuer URL                       | Yes      |
| `API_BASE_URL`      | Internal URL to backend API                     | Yes      |

*`ENCRYPTION_KEY` is required when sensitive field encryption is enabled.

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
│   │       └── scheduler/     ← Notification generation (planned)
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
