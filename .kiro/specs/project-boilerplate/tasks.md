# Implementation Plan: Project Boilerplate

## Overview

Set up the foundational project structure for CrewCaptain: Kotlin Spring Boot 3 backend (Hexagonal/DDD), Next.js frontend with Auth.js, Docker containerization, local development tooling, and environment documentation. Each task builds incrementally so the project compiles and runs at each checkpoint.

## Tasks

- [x] 1. Set up backend project structure and build configuration
  - [x] 1.1 Create Gradle wrapper and project scaffolding
    - Create `api/settings.gradle.kts` with project name `peoplemanager-api`
    - Create `api/gradle.properties` with Kotlin JVM target 21 and Spring Boot version 3.3.x
    - Initialize Gradle wrapper files (`gradlew`, `gradlew.bat`, `gradle/wrapper/`)
    - _Requirements: 1.1_

  - [x] 1.2 Create `api/build.gradle.kts` with all dependencies and plugins
    - Add plugins: `org.springframework.boot` (3.3.x), `io.spring.dependency-management`, `org.jetbrains.kotlin.jvm`, `org.jetbrains.kotlin.plugin.spring`, `org.jetbrains.kotlin.plugin.jpa`
    - Add main dependencies: spring-boot-starter-web, data-jpa, security, oauth2-resource-server, validation, flyway-core, flyway-database-postgresql, postgresql driver, jackson-module-kotlin
    - Add test dependencies: spring-boot-starter-test, spring-security-test, testcontainers (postgresql + junit-jupiter), mockk, kotest-assertions-core
    - Set Kotlin/JVM target to 21
    - _Requirements: 1.1_

  - [x] 1.3 Create Hexagonal/DDD package structure with .gitkeep files
    - Create `api/src/main/kotlin/com/peoplemanager/domain/.gitkeep`
    - Create `api/src/main/kotlin/com/peoplemanager/application/.gitkeep`
    - Create `api/src/main/kotlin/com/peoplemanager/adapters/web/.gitkeep`
    - Create `api/src/main/kotlin/com/peoplemanager/adapters/persistence/.gitkeep`
    - Create `api/src/main/kotlin/com/peoplemanager/adapters/auth/.gitkeep`
    - Create `api/src/main/kotlin/com/peoplemanager/adapters/scheduler/.gitkeep`
    - _Requirements: 1.2, 1.3_

  - [x] 1.4 Create test source tree with .gitkeep files
    - Create `api/src/test/kotlin/com/peoplemanager/domain/.gitkeep`
    - Create `api/src/test/kotlin/com/peoplemanager/application/.gitkeep`
    - Create `api/src/test/kotlin/com/peoplemanager/adapters/web/.gitkeep`
    - Create `api/src/test/kotlin/com/peoplemanager/integration/.gitkeep`
    - _Requirements: 1.4_

  - [x] 1.5 Create Spring Boot application entry point and configuration
    - Create `api/src/main/kotlin/com/peoplemanager/PeopleManagerApplication.kt` with `@SpringBootApplication` and `main` function
    - Create `api/src/main/resources/application.yml` with datasource (DB_URL, DB_USER, DB_PASSWORD), JPA (ddl-auto: validate, open-in-view: false), Flyway enabled, OAuth2 resource server (OIDC_ISSUER_URI, OIDC_JWKS_URI), and server port 8080
    - _Requirements: 1.5, 1.6_

  - [x] 1.6 Write smoke test verifying backend compiles
    - Run `./gradlew build -x test` to confirm compilation succeeds
    - _Requirements: 1.1, 1.5_

- [x] 2. Set up frontend project structure and configuration
  - [x] 2.1 Create `frontend/package.json` with all dependencies
    - Add dependencies: next (14.x), react, react-dom (18.x), next-auth (5.x), typescript (5.x)
    - Add dev dependencies: @types/react, @types/node, jest, @testing-library/react, @testing-library/jest-dom, @playwright/test, eslint, eslint-config-next
    - Add scripts: dev, build, start, test, test:coverage, test:e2e, lint
    - _Requirements: 2.1_

  - [x] 2.2 Create TypeScript and Next.js configuration files
    - Create `frontend/tsconfig.json` with strict mode enabled and path alias `@/` → `src/`
    - Create `frontend/next.config.ts` with `output: 'standalone'` for Docker optimization
    - _Requirements: 2.5, 2.6_

  - [x] 2.3 Create frontend source directory structure
    - Create `frontend/src/app/layout.tsx` with root layout component
    - Create `frontend/src/app/page.tsx` with placeholder home page
    - Create `frontend/src/components/.gitkeep`
    - Create `frontend/src/lib/.gitkeep`
    - Create `frontend/src/types/.gitkeep`
    - _Requirements: 2.2_

  - [x] 2.4 Create test directories
    - Create `frontend/__tests__/.gitkeep`
    - Create `frontend/e2e/.gitkeep`
    - _Requirements: 2.3, 2.4_

  - [x] 2.5 Write smoke test verifying frontend builds
    - Run `npm install` and `npm run build` to confirm Next.js compiles
    - _Requirements: 2.1, 2.5, 2.6_

- [x] 3. Checkpoint - Verify project structure
  - Ensure both `api/` and `frontend/` directories have correct structure, backend compiles with Gradle, and frontend builds with npm. Ask the user if questions arise.

- [x] 4. Create backend Dockerfile
  - [x] 4.1 Write multi-stage `api/Dockerfile`
    - Stage 1 (build): Use `gradle:8-jdk21`, copy build files first for dependency caching, then copy source and run `gradle bootJar --no-daemon`
    - Stage 2 (runtime): Use `eclipse-temurin:21-jre-alpine`, create non-root user `appuser`, copy JAR from build stage, expose port 8080, set ENTRYPOINT
    - Ensure runtime accepts env vars: DB_URL, DB_USER, DB_PASSWORD, OIDC_ISSUER_URI, OIDC_JWKS_URI
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [x] 5. Create frontend Dockerfile
  - [x] 5.1 Write multi-stage `frontend/Dockerfile`
    - Stage 1 (deps): Use `node:20-alpine`, copy package.json/package-lock.json, run `npm ci`
    - Stage 2 (build): Use `node:20-alpine`, install all deps, copy source, run `npm run build`
    - Stage 3 (runtime): Use `node:20-alpine`, create non-root user `appuser`, copy standalone output + static assets + public folder, expose port 3000, set CMD
    - Ensure runtime accepts env vars: NEXTAUTH_URL, NEXTAUTH_SECRET, OIDC_CLIENT_ID, OIDC_CLIENT_SECRET, OIDC_ISSUER, API_BASE_URL
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [x] 6. Create production Docker Compose
  - [x] 6.1 Write `docker-compose.yml` with all three services
    - Define `db` service: postgres:16 image, POSTGRES_DB=crewcaptain, named volume `pgdata`, health check with `pg_isready`, port 5432 NOT exposed to host
    - Define `api` service: build from `./api`, expose port 8080, set all env vars (DB_URL=jdbc:postgresql://db:5432/crewcaptain, DB_USER, DB_PASSWORD, OIDC_ISSUER_URI, OIDC_JWKS_URI, ENCRYPTION_KEY), depends_on db (service_healthy), health check with curl to actuator/health
    - Define `frontend` service: build from `./frontend`, expose port 3000, set all env vars (NEXTAUTH_URL, NEXTAUTH_SECRET, OIDC_CLIENT_ID, OIDC_CLIENT_SECRET, OIDC_ISSUER, API_BASE_URL), depends_on api (service_healthy)
    - Define named volume `pgdata`
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8_

- [x] 7. Create local development Docker Compose override
  - [x] 7.1 Write `docker-compose.override.yml` for local development
    - Expose db port 5432 to host
    - Mount `./api/src` into api container for live reloading
    - Mount `./frontend/src` and `./frontend/public` into frontend container
    - Set development environment variables (SPRING_PROFILES_ACTIVE=dev, SPRING_DEVTOOLS_RESTART_ENABLED=true, NODE_ENV=development)
    - Override frontend command to `npm run dev`
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 8. Checkpoint - Verify Docker configuration
  - Ensure docker-compose.yml is valid YAML, all services reference correct build contexts, health checks are properly configured, and service dependency chain is correct (db → api → frontend). Ask the user if questions arise.

- [x] 9. Create local development script
  - [x] 9.1 Write `dev.sh` with backend and frontend commands
    - Add shebang `#!/usr/bin/env bash` and `set -euo pipefail`
    - Implement dependency checks: Java 21 for backend, Node.js/npm for frontend
    - Implement `.env` loading (`.env` for backend, `.env.local` for frontend)
    - Implement `backend` command: run `./gradlew build -x test`, apply Flyway migrations, start with `./gradlew bootRun` (continuous build for hot-reload)
    - Implement `frontend` command: run `npm install`, start with `npm run dev`
    - Implement usage message when called without valid argument
    - Make script executable (`chmod +x`)
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

  - [x] 9.2 Write tests for dev.sh
    - Test that `./dev.sh` without arguments prints usage and exits non-zero
    - Test that `./dev.sh invalid` prints usage and exits non-zero
    - _Requirements: 3.3_

- [x] 10. Create environment variable documentation
  - [x] 10.1 Write `.env.example` with all documented variables
    - Add section header comments for Backend and Frontend
    - Add DB_URL, DB_USER, DB_PASSWORD with descriptive comments and placeholder values
    - Add OIDC_ISSUER_URI, OIDC_JWKS_URI, ENCRYPTION_KEY with descriptive comments
    - Add NEXTAUTH_URL, NEXTAUTH_SECRET, OIDC_CLIENT_ID, OIDC_CLIENT_SECRET, OIDC_ISSUER, API_BASE_URL with descriptive comments
    - Use clearly non-functional placeholder values (e.g., `your-oidc-issuer-url-here`)
    - _Requirements: 8.1, 8.2, 8.3, 8.4_

- [x] 11. Create root project files
  - [x] 11.1 Create `.gitignore` for the monorepo
    - Add entries for: `.env`, `docker-compose.override.yml`, `node_modules/`, `.next/`, `build/`, `.gradle/`, `*.jar`, IDE files
    - _Requirements: 7.1 (override gitignored)_

  - [x] 11.2 Write integration test for Spring Boot application context
    - Create `api/src/test/kotlin/com/peoplemanager/integration/ApplicationContextTest.kt`
    - Use Testcontainers PostgreSQL to verify Spring Boot context loads successfully
    - _Requirements: 1.5_

- [x] 12. Final checkpoint - Verify complete boilerplate
  - Ensure all tests pass, verify folder structure matches design, confirm docker-compose.yml is valid, and dev.sh is executable. Ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Database name is `crewcaptain` (not `peoplemanager`) as specified in design
- No property-based tests for this feature — it's infrastructure/config only
- The `docker-compose.override.yml` should be gitignored (local dev only)
- All environment variables use placeholder defaults for documentation clarity
