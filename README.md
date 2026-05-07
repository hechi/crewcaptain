# CrewCaptain

[![License: AGPL-3.0](https://img.shields.io/badge/License-AGPL--3.0-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)

A self-hosted, privacy-first manager workspace for organizing people context, 1:1 history, development goals, action items, and kudos. CrewCaptain is not HR software — it's a private cockpit for people-centric leadership.

---

## Features

- **1:1 Management** — Capture and review 1:1 meeting history per team member
- **PDP Tracking** — Track personal development goals with status transitions
- **Action Items** — Create, assign, and track follow-ups from meetings
- **Kudos** — Record positive feedback and achievements
- **Morale Tracking** — Visual morale indicators (Green/Yellow/Red)
- **Sensitive Content** — Flag and hide sensitive notes with encryption support
- **OIDC Authentication** — Secure login via authentik (OAuth2/OIDC)
- **Data Ownership** — Self-hosted with full data export capabilities

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

## authentik Setup (OIDC)

1. In your authentik admin panel, create a new **OAuth2/OIDC Provider**:
   - Name: `crewcaptain`
   - Client type: Confidential
   - Redirect URIs: `http://localhost:3000/api/auth/callback/authentik`
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
│   │   ├── domain/            ← Aggregates, Value Objects
│   │   ├── application/       ← Use Cases, Ports
│   │   └── adapters/          ← web, persistence, auth, scheduler
│   ├── src/test/kotlin/       ← Tests (domain, application, integration)
│   ├── build.gradle.kts
│   └── Dockerfile
│
└── frontend/                  ← Next.js frontend
    ├── src/app/               ← App Router pages
    ├── src/components/        ← Reusable UI components
    ├── src/lib/               ← API client, auth helpers
    ├── src/types/             ← TypeScript type definitions
    ├── __tests__/             ← Jest + React Testing Library
    ├── e2e/                   ← Playwright end-to-end tests
    ├── package.json
    └── Dockerfile
```

---

## Contributing

1. Read `AGENTS.md` for the full development workflow and architecture rules
2. Follow the mandatory workflow: Read → Plan → Test → Code → Verify → Docs → Log
3. Every change must include tests — no exceptions
4. All data queries must be scoped by `userId` (security invariant)
5. Update `README.md` and `PROGRESS.md` with every change

---

## License

This project is licensed under the [GNU Affero General Public License v3.0](https://www.gnu.org/licenses/agpl-3.0.html).
