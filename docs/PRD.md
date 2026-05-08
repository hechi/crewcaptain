# People Manager Workspace (Open Source) — Product Description / PRD (v0.2)

> **Purpose:** A self-hosted, multi-user (multi-manager) “manager-only CRM” for running effective 1:1s, tracking development, capturing notes, and staying on top of follow-ups.  
> **License:** AGPL-3.0  
> **Target deployment:** Docker Compose (no Kubernetes/Helm requirement)

---

## 1) Problem Statement

People managers accumulate critical context about their team across meeting notes, docs, chats, and task tools. This causes:
- missed follow-ups and coaching opportunities
- inconsistent 1:1 structure per person
- loss of institutional memory over time
- time wasted preparing for 1:1s and review cycles

This tool centralizes manager-only context with fast workflows and an “at-a-glance” person view.

---

## 2) Product Goals (MVP)

1. **Run better 1:1s faster**
   - quick capture during/after 1:1s
   - per-person cadence and recurring structure
2. **Never lose follow-ups**
   - action items with due dates + in-app reminders
3. **Track development over time**
   - PDP goals + progress updates visible on the person page
4. **Manager memory augmentation**
   - pinned “remember” items
   - last 1:1 date, open items, morale, anniversaries
5. **Self-hosted + data-owned**
   - export to Markdown (per person)
6. **Multi-manager in one instance**
   - each manager sees only their own data by default

---

## 3) Non-Goals (Explicit)

- HRIS replacement (Workday/BambooHR)
- compensation planning, calibration, formal performance ratings
- shared agendas/notes with team members (manager-only system)
- Slack/Teams/Calendar/Jira integrations (not in MVP)
- automatic retention/deletion (records kept indefinitely)

---

## 4) Users & Access Model

### 4.1 Users
- **Manager (User):** primary (and only) interactive role in MVP

### 4.2 Multi-manager isolation
- Default assumption: **manager data is private to that manager**.
- No “org structure” is required (no org chart, no dotted-line hierarchy).

### 4.3 Authentication / Authorization
- **SSO:** OAuth2 / OIDC via **authentik**
- Frontend handles login (Next.js auth layer), backend validates access tokens.

**Auth requirements:**
- OIDC discovery / JWKS validation (backend)
- user identity mapped by stable subject (`sub`) + issuer (`iss`)
- on first login: auto-provision `User` record

---

## 5) Core MVP Features

### 5.1 Team Member (Person) Directory (Manager-owned)
Each manager maintains their own list of people.

**Person profile fields (MVP):**
- name (required)
- preferred name (optional)
- role/title (optional)
- timezone (optional)
- start date (optional)
- email (optional; not required to be unique globally)
- tags (optional)

**At-a-glance panel on Person page:**
- last 1:1 date
- open action items count + next due date
- active PDP goals summary (top 3 + count)
- pinned “remember” bullets
- morale flag (optional enum + note)
- upcoming anniversaries (e.g., work anniversary from start date)

### 5.2 1:1 Cadence + 1:1 Notes (Manager-only)
- Per-person cadence options:
  - twice weekly, weekly, biweekly, monthly, custom (e.g., every N days)
- Generate upcoming 1:1 “suggestions” based on cadence (no calendar integration).
- Create 1:1 entry quickly from person page.

**1:1 Entry fields:**
- meeting date/time (required; default now)
- agenda items (checkbox bullets)
- notes (Markdown editor)
- outcomes/decisions (optional)
- inline action items (created while writing)
- attachments/links (optional)

**Templates:**
- per-person default 1:1 template (Markdown snippet) to prefill new entries

### 5.3 Quick Notes (“Inbox”)
- Global capture input (fast, minimal friction)
- Quick note can be:
  - unassigned (goes into inbox)
  - assigned to a person
- Actions:
  - attach to a 1:1 entry
  - convert to an action item
  - pin as “remember”
  - mark as “kudos”

### 5.4 Action Items & Follow-ups
- Action item fields:
  - title (required)
  - description (optional)
  - owner: `MANAGER` or `PERSON` (report)
  - due date (optional but recommended)
  - status: `OPEN | DONE | CANCELED`
  - links: person, originating 1:1, originating quick note

**Views:**
- My open action items
- Overdue action items
- Action items per person

### 5.5 Personal Development Plan (PDP)
Per-person PDP with goals and progress updates.

**PDP Goal fields:**
- title (required)
- description (optional)
- target date (optional)
- status: `ACTIVE | ACHIEVED | PAUSED | DROPPED`
- progress updates (timestamped Markdown notes)

### 5.6 Kudos / Recognition
Lightweight, manager-captured kudos entries tied to a person.

**Kudos fields:**
- date
- short text (Markdown)
- optional tags (e.g., “impact”, “collaboration”)

### 5.7 Morale / Pulse (Simple)
- Morale flag per person:
  - `GREEN | YELLOW | RED | UNKNOWN`
- optional note + last updated timestamp

### 5.8 In-app Notifications (No email)
- Notification center (bell/inbox)
- Notification types (MVP):
  - action item overdue
  - action item due soon (configurable threshold, default 3 days)
  - “you haven’t had a 1:1 with X in N days” (based on cadence)
  - upcoming work anniversary (if start date exists)

### 5.9 Search
Full-text search across manager-owned data:
- people
- 1:1 notes
- quick notes
- action items
- PDP goals + updates
- kudos

### 5.10 Export (Markdown)
- Export **per person** to Markdown:
  - profile summary
  - pinned remember list
  - morale
  - 1:1 history (reverse chronological)
  - action items (open + done)
  - PDP goals + updates
  - kudos
- Optional: export date range filter

---

## 6) Sensitive Notes Handling

Some notes may be sensitive (e.g., health or personal situations voluntarily shared).

**Requirement:**
- Any note-like content (1:1 entry, quick note, PDP update, morale note, etc.) can be flagged `sensitive = true`.

**Recommended (MVP+):**
- Encrypt sensitive text fields at rest using application-level encryption:
  - envelope encryption with a master key supplied via environment variable / Docker secret
  - store ciphertext + metadata (algo/version)
- If encryption is not implemented in MVP, still implement the `sensitive` flag and UI warnings.

**Explicit UX:**
- Sensitive notes clearly labeled
- Optional “hide sensitive content” toggle in UI

---

## 7) Data Model (Domain View)

> DDD-style aggregates; persistence is implementation detail.

### Aggregates / Entities
- **User (Manager)**
  - id, oidcSubject, oidcIssuer, displayName, email
- **Person**
  - belongsTo: User
  - profile fields
  - pinnedRememberItems (list)
  - moraleStatus + moraleNote
- **OneOnOneSeries**
  - belongsTo: User + Person
  - cadence config (enum + custom interval)
  - templateMarkdown
- **OneOnOneEntry**
  - belongsTo: User + Person
  - occurredAt
  - agenda (structured list)
  - notesMarkdown
  - outcomesMarkdown
  - sensitive flag
- **QuickNote**
  - belongsTo: User
  - optional Person
  - textMarkdown
  - status: `INBOX | ATTACHED | CONVERTED | ARCHIVED`
  - sensitive flag
- **ActionItem**
  - belongsTo: User + Person
  - title, description
  - ownerType (MANAGER/PERSON)
  - dueDate, status
  - links to origin (1:1 / quick note)
- **PdpGoal**
  - belongsTo: User + Person
  - fields above
- **PdpUpdate**
  - belongsTo: PdpGoal
  - updateAt, textMarkdown, sensitive flag
- **Kudos**
  - belongsTo: User + Person
  - date, textMarkdown
- **Notification**
  - belongsTo: User
  - type, payload (json), readAt, createdAt

### Multi-manager stance
- Every “content” record is scoped by `userId` (manager).
- People are manager-owned to avoid implicit org modeling and permission complexity.

---

## 8) Key Workflows

### 8.1 Before a 1:1
- Manager opens person page:
  - sees last 1:1, open action items, PDP goals, pinned remember list, morale
- clicks “Start 1:1”
- entry prefilled with template + open action items section

### 8.2 During/After a 1:1
- Add notes (Markdown)
- Create action items inline
- Optionally mark sections as sensitive (or entire entry)

### 8.3 Quick capture
- Use global “Quick note” box
- Later, from inbox:
  - attach to next 1:1
  - convert to action item
  - convert to kudos
  - pin as remember item

### 8.4 Ongoing follow-ups
- Dashboard shows:
  - due soon & overdue action items
  - “stale 1:1” alerts based on cadence
  - upcoming anniversaries

---

## 9) UI Requirements (MVP Screens)

- **Login page** (OIDC via authentik)
- **Dashboard**
  - Upcoming 1:1s (computed)
  - Overdue/soon action items
  - Stale 1:1 reminders
  - Anniversaries
- **People list**
  - filter by tag, morale status
- **Person detail**
  - at-a-glance panel + timeline
  - tabs: 1:1s, action items, PDP, kudos, notes inbox (filtered)
- **1:1 editor**
  - Markdown editor
  - inline action item creation
- **Quick Notes inbox**
- **Notifications center**
- **Settings**
  - reminder thresholds
  - export
  - encryption key status (if implemented)

---

## 10) Technical Requirements / Architecture

### 10.1 Backend
- **Language:** Kotlin
- **Architecture:** Hexagonal (Ports & Adapters) + DDD-ish layering
  - `domain` module: aggregates, value objects, domain services, invariants
  - `application` module: use cases (commands/queries), ports (interfaces)
  - `adapters`
    - `web` (REST controllers)
    - `persistence` (JPA repositories / SQL)
    - `auth` (OIDC token verification)
    - `scheduler` (notification generation)

**Recommended well-supported libraries:**
- Spring Boot 3 (Web, Security, Validation)
- Spring Security OAuth2 Resource Server (JWT validation via JWKS)
- Hibernate/JPA
- Flyway (migrations)
- PostgreSQL driver
- (Optional) Quartz or Spring Scheduling for notifications

### 10.2 Frontend
- **Framework:** React + Next.js
- **Auth layer:** Next.js integrated OIDC login against authentik
  - Recommended: Auth.js (NextAuth) OIDC provider
- **API communication:** REST with bearer token
- **UI:** any well-supported component library (e.g., MUI) is acceptable

### 10.3 API Style (MVP)
- REST + JSON
- Pagination for list endpoints
- Server-side full-text search (Postgres) or simple `ILIKE` MVP (upgrade later)

### 10.4 Deployment
- Docker Compose services:
  - `frontend` (Next.js)
  - `api` (Kotlin app)
  - `db` (Postgres)
- Configuration via environment variables:
  - OIDC issuer, client id, JWKS URL
  - DB URL/user/password
  - optional encryption master key

---

## 11) Security & Privacy Requirements

- All endpoints require authentication (OIDC JWT)
- Enforce `userId` scoping in every query (no cross-manager leakage)
- Audit log (post-MVP): record key actions (create/update/delete) for manager’s own traceability
- Sensitive notes:
  - at minimum: flag + UI warnings
  - recommended: encryption at rest for sensitive content
- Backups: documented Postgres backup/restore steps

---

## 12) MVP Acceptance Criteria (Testable)

1. Multi-manager: two users logging in via authentik cannot see each other’s people or notes.
2. A manager can create a person and see an at-a-glance panel including:
   - last 1:1 date
   - open action items count
   - active PDP goals count
   - morale status
3. A manager can create a 1:1 entry with Markdown notes and view it later in the person timeline.
4. A manager can create action items (with due date) from a 1:1 entry and see overdue items on the dashboard.
5. A manager can create PDP goals + progress updates and see them on the person page.
6. A manager can create kudos entries and view them per person.
7. Quick notes inbox exists and quick notes can be attached to a person and/or a 1:1.
8. In-app notifications are generated for overdue action items and stale 1:1s.
9. Per-person Markdown export works and includes 1:1 history, action items, PDP, kudos, pinned remember items.
10. Sensitive flag exists on note-like entities and is visible in UI; (encryption optional but recommended).

---

## 13) Suggested Roadmap (After MVP)

- Encryption-at-rest for sensitive fields (if not shipped in MVP)
- Better search (Postgres FTS + ranking + filters)
- Review packet generator (date range summaries)
- Bulk import (CSV people list)
- Optional “workspace” concept (still manager-private by default)
- Soft-delete + restore (safety)
- Audit log

---