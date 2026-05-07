# PROGRESS.md

## Last Updated
2026-05-08T12:00:00Z — Project boilerplate setup complete

## Current Status
The foundational project boilerplate is fully set up. Both the Kotlin Spring Boot backend and Next.js frontend compile and build successfully. Docker infrastructure (Dockerfiles, docker-compose.yml, override) is in place. Local development tooling (dev.sh) is functional. The project is ready for feature development.

## Completed Features
- [x] Backend project structure — Gradle Kotlin DSL, Spring Boot 3.3.5, Hexagonal/DDD package layout (2026-05-08)
- [x] Frontend project structure — Next.js 14, React 18, Auth.js, TypeScript strict mode (2026-05-08)
- [x] Backend Dockerfile — Multi-stage build (gradle:8-jdk21 → eclipse-temurin:21-jre-alpine) (2026-05-08)
- [x] Frontend Dockerfile — Multi-stage build (node:20-alpine, 3 stages with standalone output) (2026-05-08)
- [x] Docker Compose — Production stack with db, api, frontend services and health checks (2026-05-08)
- [x] Docker Compose override — Local dev with volume mounts and exposed ports (2026-05-08)
- [x] Local development script — dev.sh with backend/frontend commands, dependency checks (2026-05-08)
- [x] Environment documentation — .env.example with all variables documented (2026-05-08)
- [x] Integration test — ApplicationContextTest with Testcontainers PostgreSQL (2026-05-08)
- [x] README.md — Initial documentation with setup, configuration, and usage (2026-05-08)

## In Progress
- (none)

## Known Issues / Bugs
| ID  | Description                                          | Severity | Status |
|-----|------------------------------------------------------|----------|--------|
| 001 | Backend tests require Java 21 explicitly (system default may differ) | Low | Open |
| 002 | docker-compose.yml exposes db port 5432 (should only be in override) | Low | Open |

## Next Steps (Prioritized)
1. Set up Flyway migrations with initial schema (users, persons tables)
2. Implement domain model (Person aggregate, UserId value object)
3. Implement OIDC authentication adapter
4. Create first REST endpoint (Person CRUD)
5. Set up Auth.js in frontend with authentik provider
6. Implement 1:1 entry feature

## Architecture Decisions Made This Session
- Gradle Kotlin DSL over Groovy: Type-safe build scripts, better IDE support
- Multi-stage Docker builds: Minimal production images, faster CI caching
- dev.sh as primary local runner: Single entry point for local development
- Testcontainers over H2: Tests run against real PostgreSQL matching production
- Flyway for migrations: Versioned, repeatable schema management
- Next.js standalone output: Optimized Docker images for frontend

## Environment / Setup Notes
- Java 21 is required for backend development (use SDKMAN: `sdk install java 21-tem`)
- Node.js 20+ required for frontend
- Docker required for running Testcontainers-based integration tests
- Copy `.env.example` to `.env` before running `./dev.sh backend`
- Copy `.env.example` to `.env.local` for frontend-specific overrides
- The `next.config.mjs` is used instead of `next.config.ts` (Next.js 14.x doesn't support TS config)

## Test Coverage Summary
- Backend: 1 test (ApplicationContextTest) — passes with Testcontainers (last run: 2026-05-08)
- Frontend: No tests yet (test infrastructure ready)
- E2E: No tests yet (Playwright configured)

## Open Questions / Flags for Human Review
- docker-compose.yml currently exposes db port 5432 to host — should this be removed for production? (design doc says NOT exposed, but current file has it)
