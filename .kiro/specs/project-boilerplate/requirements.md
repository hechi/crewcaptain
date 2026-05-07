# Requirements Document

## Introduction

This document defines the requirements for the initial project boilerplate setup of CrewCaptain. The boilerplate establishes the foundational folder structure, build configuration, containerization, and local development tooling for both the Kotlin Spring Boot backend (Hexagonal/DDD architecture) and the React Next.js frontend. It ensures developers can clone the repository and begin productive work immediately using either local development (`dev.sh`) or Docker Compose environments.

## Glossary

- **Dev_Script**: The `dev.sh` shell script that serves as the primary local development runner, supporting `./dev.sh backend` and `./dev.sh frontend` commands.
- **Backend_Service**: The Kotlin Spring Boot 3 application following Hexagonal/DDD architecture, serving the REST API on port 8080.
- **Frontend_Service**: The React Next.js application with Auth.js integration, serving the web UI on port 3000.
- **Database_Service**: The PostgreSQL 16 database instance used for persistent storage, accessible on port 5432 internally.
- **Docker_Compose_Stack**: The set of containerized services defined in `docker-compose.yml` for production-like environments.
- **Local_Override**: The `docker-compose.override.yml` file providing local development overrides (volume mounts, debug ports, relaxed settings).
- **Hexagonal_Structure**: The backend package layout separating domain, application, and adapter layers with strict inward dependency direction.
- **Hot_Reload**: The ability for code changes to be reflected in the running service without manual restart.

## Requirements

### Requirement 1: Backend Folder Structure

**User Story:** As a developer, I want the backend project to follow a Hexagonal/DDD folder structure with Kotlin and Spring Boot 3, so that I can implement features with clear separation of concerns from day one.

#### Acceptance Criteria

1. THE Backend_Service SHALL contain a Gradle Kotlin DSL build file (`build.gradle.kts`) at `api/build.gradle.kts` with Spring Boot 3, Kotlin, and PostgreSQL dependencies configured.
2. THE Backend_Service SHALL contain the package structure `com.peoplemanager` under `api/src/main/kotlin/` with subdirectories for `domain`, `application`, and `adapters`.
3. THE Backend_Service SHALL contain adapter subdirectories for `web`, `persistence`, `auth`, and `scheduler` under the `adapters` package.
4. THE Backend_Service SHALL contain a test source tree at `api/src/test/kotlin/com/peoplemanager/` with subdirectories for `domain`, `application`, `adapters/web`, and `integration`.
5. THE Backend_Service SHALL include a Spring Boot application entry point class that starts successfully with a valid configuration.
6. THE Backend_Service SHALL include an `application.yml` configuration file referencing environment variables for database connection and OIDC settings.

### Requirement 2: Frontend Folder Structure

**User Story:** As a developer, I want the frontend project to follow the Next.js App Router convention with Auth.js pre-configured, so that I can build authenticated pages immediately.

#### Acceptance Criteria

1. THE Frontend_Service SHALL contain a `package.json` at `frontend/package.json` with React, Next.js, Auth.js, and TypeScript as dependencies.
2. THE Frontend_Service SHALL contain the source directory structure under `frontend/src/` with subdirectories for `app`, `components`, `lib`, and `types`.
3. THE Frontend_Service SHALL contain a test directory at `frontend/__tests__/` for Jest and React Testing Library tests.
4. THE Frontend_Service SHALL contain an end-to-end test directory at `frontend/e2e/` for Playwright tests.
5. THE Frontend_Service SHALL include a TypeScript configuration file (`tsconfig.json`) with strict mode enabled.
6. THE Frontend_Service SHALL include a Next.js configuration file (`next.config.js` or `next.config.ts`) with appropriate settings for the application.

### Requirement 3: Local Development Script

**User Story:** As a developer, I want a single `dev.sh` script that handles dependency installation, migrations, and hot-reload startup, so that I can begin local development with one command after cloning.

#### Acceptance Criteria

1. WHEN invoked with the argument `backend`, THE Dev_Script SHALL install backend dependencies, apply database migrations via Flyway, and start the Spring Boot application with hot-reload enabled.
2. WHEN invoked with the argument `frontend`, THE Dev_Script SHALL install frontend dependencies via npm and start the Next.js development server with hot-reload enabled.
3. WHEN invoked without a valid argument, THE Dev_Script SHALL display a usage message listing the available commands (`backend`, `frontend`).
4. THE Dev_Script SHALL load environment variables from a `.env` file (or `.env.local` for the frontend) when present in the project root.
5. THE Dev_Script SHALL be executable (`chmod +x`) and use `#!/usr/bin/env bash` as the shebang line.
6. IF a required dependency tool is missing (Java, Node.js, npm), THEN THE Dev_Script SHALL display a descriptive error message and exit with a non-zero status code.

### Requirement 4: Backend Dockerfile

**User Story:** As a DevOps engineer, I want a multi-stage Dockerfile for the backend, so that the production image is minimal and secure.

#### Acceptance Criteria

1. THE Backend_Service SHALL include a `Dockerfile` at `api/Dockerfile` that produces a runnable container image.
2. THE Backend_Service Dockerfile SHALL use a multi-stage build with a Gradle build stage and a minimal JRE runtime stage.
3. THE Backend_Service Dockerfile SHALL expose port 8080.
4. THE Backend_Service Dockerfile SHALL accept environment variables for `DB_URL`, `DB_USER`, `DB_PASSWORD`, `OIDC_ISSUER_URI`, and `OIDC_JWKS_URI` at runtime.
5. THE Backend_Service Dockerfile SHALL use a non-root user to run the application process.

### Requirement 5: Frontend Dockerfile

**User Story:** As a DevOps engineer, I want a multi-stage Dockerfile for the frontend, so that the production image is optimized and secure.

#### Acceptance Criteria

1. THE Frontend_Service SHALL include a `Dockerfile` at `frontend/Dockerfile` that produces a runnable container image.
2. THE Frontend_Service Dockerfile SHALL use a multi-stage build with a Node.js build stage and a minimal runtime stage.
3. THE Frontend_Service Dockerfile SHALL expose port 3000.
4. THE Frontend_Service Dockerfile SHALL accept environment variables for `NEXTAUTH_URL`, `NEXTAUTH_SECRET`, `OIDC_CLIENT_ID`, `OIDC_CLIENT_SECRET`, `OIDC_ISSUER`, and `API_BASE_URL` at runtime.
5. THE Frontend_Service Dockerfile SHALL use a non-root user to run the application process.

### Requirement 6: Production Docker Compose

**User Story:** As a DevOps engineer, I want a `docker-compose.yml` that defines all services for a production-like deployment, so that the full stack can be started with a single command.

#### Acceptance Criteria

1. THE Docker_Compose_Stack SHALL define three services: `frontend`, `api`, and `db`.
2. THE Docker_Compose_Stack SHALL configure the `frontend` service to expose port 3000 to the host.
3. THE Docker_Compose_Stack SHALL configure the `api` service to expose port 8080 to the host.
4. THE Docker_Compose_Stack SHALL configure the `db` service using the `postgres:16` image with port 5432 accessible only to other services (not exposed to the host).
5. THE Docker_Compose_Stack SHALL define a named volume for PostgreSQL data persistence.
6. THE Docker_Compose_Stack SHALL configure service dependencies so that `api` starts after `db` is healthy, and `frontend` starts after `api` is available.
7. THE Docker_Compose_Stack SHALL reference all required environment variables from AGENTS.md section 9.2 with placeholder values.
8. THE Docker_Compose_Stack SHALL include health checks for the `db` and `api` services.

### Requirement 7: Local Development Docker Compose Override

**User Story:** As a developer, I want a `docker-compose.override.yml` for local development that mounts source code and enables debugging, so that I can use Docker Compose locally with fast feedback loops.

#### Acceptance Criteria

1. THE Local_Override SHALL mount the `api/` source directory into the `api` container for live code reloading.
2. THE Local_Override SHALL mount the `frontend/` source directory into the `frontend` container for live code reloading.
3. THE Local_Override SHALL expose the PostgreSQL port 5432 to the host for direct database access during development.
4. THE Local_Override SHALL set development-appropriate environment variable values (local database credentials, localhost URLs).
5. THE Local_Override SHALL disable production optimizations (minification, caching) where applicable.

### Requirement 8: Environment Variable Documentation

**User Story:** As a developer, I want a `.env.example` file documenting all required environment variables with placeholder values, so that I can quickly configure my local environment.

#### Acceptance Criteria

1. THE Backend_Service SHALL be documented in `.env.example` with entries for `DB_URL`, `DB_USER`, `DB_PASSWORD`, `OIDC_ISSUER_URI`, `OIDC_JWKS_URI`, and `ENCRYPTION_KEY`.
2. THE Frontend_Service SHALL be documented in `.env.example` with entries for `NEXTAUTH_URL`, `NEXTAUTH_SECRET`, `OIDC_CLIENT_ID`, `OIDC_CLIENT_SECRET`, `OIDC_ISSUER`, and `API_BASE_URL`.
3. THE `.env.example` file SHALL include descriptive comments for each variable explaining its purpose and expected format.
4. THE `.env.example` file SHALL use clearly non-functional placeholder values (not real credentials or URLs that could be mistaken for production values).
