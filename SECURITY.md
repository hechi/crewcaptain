# Security Policy

CrewCaptain is a self-hosted workspace that stores sensitive people-management
data (1:1 notes, morale, development plans, personal observations). Security and
privacy are taken seriously and are core design goals of the project.

> **Please set expectations accordingly.** CrewCaptain is built and maintained
> by a **single developer in their spare time**, offered as-is under the
> AGPL-3.0 (see [LICENSE](LICENSE), sections 15–16 — no warranty). There is no
> security team, no SLA, and no guarantee of a response time or a fix. I will do
> my best to look into and address reports, but you should treat this project as
> a best-effort, community open-source effort — review the code and run your own
> assessment before relying on it for anything important.

## Reporting a vulnerability

**Please do not open a public issue for security vulnerabilities.**

Instead, report privately through one of:

- GitHub's [private vulnerability reporting](https://docs.github.com/en/code-security/security-advisories/guidance-on-reporting-and-writing-information-about-vulnerabilities/privately-reporting-a-security-vulnerability)
  (the "Report a vulnerability" button under the repository's **Security** tab), or
- Email the maintainer at **world@greenstation.de**.

Please include:

- A description of the issue and its potential impact.
- Steps to reproduce, or a proof of concept.
- Affected version / commit and your environment.

I'll acknowledge reports as soon as I reasonably can, but since this is a
spare-time project, please be patient — there is no guaranteed turnaround. When
a confirmed issue is fixed, I'm happy to credit you in the release notes unless
you'd prefer to stay anonymous.

## Supported versions

This is an actively (but casually) developed project with no formal release
train. Security fixes land on the `main` branch as I get to them. If you
self-host, track `main` (or the latest published images) to stay current —
older checkouts are not patched retroactively.

## Scope & expectations

The following are the security-critical guarantees the project aims to uphold.
Reports demonstrating a break in any of these are especially valuable:

- **Data isolation** — a manager must never be able to read or modify another
  manager's data. All queries are scoped by `userId`; cross-manager access
  returns `404`.
- **Authentication** — all `/api/v1/` endpoints require a valid OIDC bearer
  token. Unauthenticated requests return `401`.
- **Encryption at rest** — content marked `sensitive=true` is encrypted with
  AES-256-GCM when `ENCRYPTION_KEY` is configured.
- **Sensitive-data handling** — sensitive content is excluded from exports,
  review packets, search snippets, and (in Privacy Mode) AI/LLM requests.
- **Metrics protection** — the `/actuator/prometheus` endpoint is gated behind
  a bearer token (`METRICS_TOKEN`).

## Deployment hardening notes

When self-hosting, please:

- Set strong, unique values for `DB_PASSWORD`, `NEXTAUTH_SECRET`,
  `ENCRYPTION_KEY`, and `METRICS_TOKEN`. **Never** ship the example/default
  values (e.g. `changeme`, `change-me-in-production`) to production.
- Configure `ENCRYPTION_KEY` so sensitive fields are encrypted at rest.
- Never enable the `docker-compose.dev-auth.yml` overlay in production — it
  ships well-known demo credentials for local development only.
- Serve the application over HTTPS behind a trusted reverse proxy.

Thank you for helping keep CrewCaptain and its users safe.
