# Implementation Plan: 1:1 Entry Management

## Overview

This plan implements the 1:1 Entry Management feature for CrewCaptain — the core workflow enabling managers to configure meeting cadences, create structured 1:1 entries with Markdown notes and agenda items, and track meeting history per person. Implementation follows the hexagonal/DDD architecture, progressing from database schema through domain, application, adapters, and frontend.

## Tasks

- [ ] 1. Database migrations
  - [ ] 1.1 Create Flyway migration for one_on_one_series table
    - Create `api/src/main/resources/db/migration/V20250508120003__create_one_on_one_series_table.sql`
    - Define columns: id (UUID PK), user_id (FK to users), person_id (FK to persons), cadence_type (VARCHAR), custom_interval_days (INTEGER nullable), template_markdown (TEXT nullable), created_at, updated_at
    - Add unique constraint on (user_id, person_id)
    - Add index on (user_id, person_id)
    - _Requirements: 10.1, 10.4_

  - [ ] 1.2 Create Flyway migration for one_on_one_entries table
    - Create `api/src/main/resources/db/migration/V20250508120004__create_one_on_one_entries_table.sql`
    - Define columns: id (UUID PK), user_id (FK to users), person_id (FK to persons), meeting_date (TIMESTAMP WITH TIME ZONE NOT NULL), notes_markdown (TEXT nullable), outcomes_markdown (TEXT nullable), sensitive (BOOLEAN default false), created_at, updated_at
    - Add index on (user_id, person_id) and (person_id, meeting_date DESC)
    - _Requirements: 10.2, 10.5_

  - [ ] 1.3 Create Flyway migration for agenda_items table
    - Create `api/src/main/resources/db/migration/V20250508120005__create_agenda_items_table.sql`
    - Define columns: id (UUID PK), entry_id (FK to one_on_one_entries with CASCADE delete), text (TEXT NOT NULL), checked (BOOLEAN default false), display_order (INTEGER), created_at
    - Add index on (entry_id)
    - _Requirements: 10.3, 10.6_

  - [ ] 1.4 Write Flyway migration integration test
    - Extend existing `FlywayMigrationTest.kt` or create new test
    - Verify all new migrations apply cleanly against Testcontainers PostgreSQL
    - Verify table structures, constraints, and indexes match expected schema
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6_

- [ ] 2. Domain layer — Value Objects and Aggregates
  - [ ] 2.1 Implement Value Objects
    - Create `OneOnOneSeriesId`, `OneOnOneEntryId`, `AgendaItemId` value classes wrapping UUID
    - Create `CadenceType` enum with WEEKLY, BIWEEKLY, MONTHLY, CUSTOM values
    - Place in `com.peoplemanager.domain` package
    - _Requirements: 1.1, 7.2_

  - [ ] 2.2 Implement OneOnOneSeries aggregate root
    - Create `OneOnOneSeries` data class with all fields per design
    - Add invariant: if cadenceType == CUSTOM, customIntervalDays must be positive
    - _Requirements: 1.1, 1.5_

  - [ ] 2.3 Implement OneOnOneEntry aggregate root
    - Create `OneOnOneEntry` data class with all fields per design
    - Add invariant: all agenda items must have non-blank text
    - Implement helper methods: updateNotes, updateOutcomes, toggleSensitive, updateAgendaItems
    - _Requirements: 2.1, 2.3, 7.1, 7.2, 8.1_

  - [ ] 2.4 Implement AgendaItem entity
    - Create `AgendaItem` data class with id, text, checked, displayOrder, createdAt
    - Add `require(text.isNotBlank())` invariant
    - _Requirements: 7.2, 7.5_

  - [ ] 2.5 Write domain unit tests
    - Test OneOnOneSeries: custom cadence requires positive interval, non-custom cadence allows null interval
    - Test OneOnOneEntry: blank agenda item text rejected, valid entry construction succeeds
    - Test AgendaItem: blank text rejected
    - Test helper methods: updateNotes, updateOutcomes, toggleSensitive, updateAgendaItems
    - _Requirements: 1.5, 7.2, 7.5, 8.1_

  - [ ] 2.6 Write property test: Custom cadence requires positive interval (Property 9)
    - Generate arbitrary non-positive integers (0, negative); verify OneOnOneSeries construction throws
    - Generate arbitrary positive integers; verify construction succeeds
    - Minimum 100 iterations
    - **Validates: Requirements 1.5**

  - [ ] 2.7 Write property test: Agenda item text non-blank invariant (Property 8)
    - Generate arbitrary whitespace-only strings; verify AgendaItem construction throws
    - Generate arbitrary non-blank strings; verify construction succeeds
    - Minimum 100 iterations
    - **Validates: Requirements 7.2, 7.5**

- [ ] 3. Application layer — Use Cases and Ports
  - [ ] 3.1 Define port interfaces
    - Create `OneOnOneSeriesRepository` interface (findByUserIdAndPersonId, save)
    - Create `OneOnOneEntryRepository` interface (save, findByIdAndUserIdAndPersonId, findAllByUserIdAndPersonId, deleteByIdAndUserIdAndPersonId, findLatestMeetingDate)
    - Create `OneOnOneCommandPort` and `OneOnOneQueryPort` inbound port interfaces
    - All methods that return data MUST accept userId parameter
    - Place in `com.peoplemanager.application.ports` package
    - _Requirements: 3.2, 5.2, 6.5_

  - [ ] 3.2 Define command and query data classes
    - Create `UpsertSeriesCommand`, `CreateEntryCommand`, `UpdateEntryCommand`, `DeleteEntryCommand`
    - Create `GetSeriesQuery`, `GetEntryQuery`, `ListEntriesQuery`, `GetLastDateQuery`
    - Place in `com.peoplemanager.application.commands` and `com.peoplemanager.application.queries` packages
    - _Requirements: 1.1, 2.1, 4.1, 5.1, 6.1_

  - [ ] 3.3 Implement OneOnOneService (all use cases)
    - UpsertSeries: validate person belongs to user, create or update series
    - CreateEntry: validate person belongs to user, apply template if notes absent, save entry
    - UpdateEntry: load entry by id+userId+personId, apply updates, save
    - DeleteEntry: delete by id+userId+personId, throw NotFoundException if not found
    - GetSeries: find by userId+personId
    - GetEntry: find by id+userId+personId, throw NotFoundException if not found
    - ListEntries: validate person belongs to user, delegate to repository with pagination
    - GetLastOneOnOneDate: delegate to repository
    - _Requirements: 1.1–1.7, 2.1–2.7, 3.1–3.3, 4.1–4.4, 5.1–5.4, 6.1–6.6, 9.1–9.3_

  - [ ] 3.4 Write use case unit tests with Mockk
    - Test UpsertSeries: creates new series, updates existing series, validates person ownership
    - Test CreateEntry: saves entry, applies template when notes absent, skips template when notes provided
    - Test UpdateEntry: updates fields, throws 404 for wrong user/person
    - Test DeleteEntry: deletes entry, throws 404 for wrong user/person
    - Test GetEntry: returns entry, throws 404 for wrong user/person
    - Test ListEntries: validates person ownership, returns paginated results
    - Test GetLastOneOnOneDate: returns latest date or null
    - _Requirements: 1.1–1.7, 2.1–2.7, 3.1–3.3, 4.1–4.4, 5.1–5.4, 6.1–6.6, 9.1–9.3_

  - [ ] 3.5 Write property test: Series upsert idempotence (Property 1)
    - Generate arbitrary valid series configs; upsert multiple times; verify single record with latest values
    - Minimum 100 iterations
    - **Validates: Requirements 1.1, 1.2, 1.7**

  - [ ] 3.6 Write property test: Entry creation round-trip (Property 2)
    - Generate valid CreateEntryCommands; create then retrieve; verify all fields match
    - Minimum 100 iterations
    - **Validates: Requirements 2.1, 2.3, 2.6, 3.1**

  - [ ] 3.7 Write property test: Template prefill when notes absent (Property 3)
    - Generate series with template + entry creation without notes; verify notes == template
    - Minimum 100 iterations
    - **Validates: Requirements 2.4**

  - [ ] 3.8 Write property test: Template NOT applied when notes provided (Property 4)
    - Generate series with template + entry creation with explicit notes; verify notes == provided value
    - Minimum 100 iterations
    - **Validates: Requirements 2.4**

- [x] 4. Checkpoint — Ensure domain and application tests pass
  - Run `cd api && ./gradlew test --tests "com.peoplemanager.domain.*" --tests "com.peoplemanager.application.*"`
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Persistence adapter
  - [x] 5.1 Implement JPA entities
    - Create `OneOnOneSeriesEntity` with JPA annotations mapping to one_on_one_series table
    - Create `OneOnOneEntryEntity` with JPA annotations mapping to one_on_one_entries table
    - Create `AgendaItemEntity` with JPA annotations mapping to agenda_items table
    - Implement bidirectional mapping between domain objects and JPA entities
    - OneOnOneEntryEntity should have @OneToMany cascade to AgendaItemEntity
    - _Requirements: 10.1, 10.2, 10.3_

  - [x] 5.2 Implement Spring Data repositories
    - Create `SpringDataOneOnOneSeriesRepository` (JpaRepository)
    - Create `SpringDataOneOnOneEntryRepository` (JpaRepository) with custom query methods
    - _Requirements: 6.4, 10.4, 10.5_

  - [x] 5.3 Implement JpaOneOnOneSeriesRepositoryAdapter
    - Implement `OneOnOneSeriesRepository` port interface
    - Map between domain `OneOnOneSeries` and JPA entity
    - _Requirements: 1.2_

  - [x] 5.4 Implement JpaOneOnOneEntryRepositoryAdapter
    - Implement `OneOnOneEntryRepository` port interface
    - All queries MUST include user_id in WHERE clause
    - Implement pagination with default sort by meeting_date DESC
    - Implement findLatestMeetingDate query
    - Map between domain `OneOnOneEntry` (with AgendaItems) and JPA entities
    - _Requirements: 3.2, 5.2, 6.4, 6.5, 9.1_

  - [ ] 5.5 Write persistence integration tests with Testcontainers
    - Test OneOnOneSeriesRepository: save, findByUserIdAndPersonId, unique constraint enforcement
    - Test OneOnOneEntryRepository: save with agenda items, findByIdAndUserIdAndPersonId, pagination, ordering
    - Test userId scoping: verify User A cannot access User B's entries
    - Test cascade delete: entry deletion removes agenda items, person deletion removes entries
    - Test findLatestMeetingDate returns correct value
    - _Requirements: 3.2, 5.2, 6.4, 6.5, 9.1, 10.4, 10.5_

- [x] 6. Web adapter — REST controller
  - [x] 6.1 Implement request/response DTOs
    - Create request DTOs: UpsertSeriesRequest, CreateEntryRequest, UpdateEntryRequest
    - Create response DTOs: OneOnOneSeriesResponse, OneOnOneEntryResponse, AgendaItemResponse
    - Add Bean Validation annotations (@NotNull, @NotBlank, etc.)
    - _Requirements: 7.5, 8.2, 11.9_

  - [x] 6.2 Implement OneOnOneController
    - PUT /api/v1/persons/{personId}/one-on-one-series — upsert series (200 OK)
    - GET /api/v1/persons/{personId}/one-on-one-series — get series (200 OK or 404)
    - POST /api/v1/persons/{personId}/one-on-one-entries — create entry (201 Created)
    - GET /api/v1/persons/{personId}/one-on-one-entries/{entryId} — get entry (200 OK)
    - PUT /api/v1/persons/{personId}/one-on-one-entries/{entryId} — update entry (200 OK)
    - DELETE /api/v1/persons/{personId}/one-on-one-entries/{entryId} — delete entry (204 No Content)
    - GET /api/v1/persons/{personId}/one-on-one-entries — list entries with pagination (200 OK)
    - Extract userId from SecurityContext for all operations
    - _Requirements: 11.1–11.9_

  - [x] 6.3 Update PersonController to include last1on1Date in at-a-glance
    - When returning a Person response, query for the latest meeting date
    - Populate the `atAGlance.last1on1Date` field (previously null placeholder)
    - _Requirements: 9.1, 9.2, 9.3_

  - [x] 6.4 Write controller slice tests (@WebMvcTest)
    - Test all series endpoints (upsert, get) with MockMvc
    - Test all entry endpoints (create, get, update, delete, list) with MockMvc
    - Test validation: blank agenda item text returns 400, invalid cadence returns 400
    - Test authentication required: requests without JWT return 401
    - Test not-found scenarios return 404
    - Test response format matches API contract
    - _Requirements: 11.1–11.9_

  - [ ] 6.5 Write property test: Authentication required on all 1:1 endpoints (Property 13)
    - Generate arbitrary 1:1 API paths; verify unauthenticated requests return 401
    - Minimum 100 iterations
    - **Validates: Requirements 11.8**

- [x] 7. Checkpoint — Ensure all backend tests pass
  - Run `cd api && ./gradlew test`
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 8. Integration tests — Full stack with security
  - [ ] 8.1 Write data isolation integration test (Property 5)
    - Create two users with persons and entries; verify User B cannot access User A's entries
    - Verify list endpoint returns only authenticated user's entries
    - Cross-user access returns 404 (not 403)
    - Use Testcontainers + full Spring context
    - **Validates: Requirements 1.6, 2.7, 3.2, 3.3, 4.3, 5.2, 5.3, 6.5, 6.6**

  - [ ] 8.2 Write pagination and ordering integration test (Properties 6, 7)
    - Create N entries with various dates; verify pagination metadata correct
    - Verify entries returned in reverse chronological order
    - **Validates: Requirements 6.1, 6.2, 6.3, 6.4**

  - [ ] 8.3 Write template prefill integration test (Properties 3, 4)
    - Create series with template; create entry without notes → verify prefill
    - Create entry with explicit notes → verify template NOT applied
    - **Validates: Requirements 2.4**

  - [ ] 8.4 Write delete-then-retrieve integration test (Property 10)
    - Create an entry, delete it, verify GET returns 404
    - **Validates: Requirements 5.1, 5.4**

  - [ ] 8.5 Write last 1:1 date integration test (Property 11)
    - Create entries with various dates; verify Person at-a-glance returns max date
    - Delete all entries; verify null returned
    - **Validates: Requirements 9.1, 9.2, 9.3**

  - [ ] 8.6 Write sensitive flag integration test (Property 12)
    - Create entry with sensitive=true; verify retrieval returns sensitive=true
    - Update to sensitive=false; verify retrieval returns sensitive=false
    - **Validates: Requirements 8.1, 8.2, 8.3**

- [ ] 9. Checkpoint — Ensure all backend tests pass including integration
  - Run `cd api && ./gradlew test`
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 10. Frontend — Types and API client
  - [ ] 10.1 Define TypeScript types
    - Add to `frontend/src/types/` or extend existing types:
      - `OneOnOneSeries` (id, personId, cadenceType, customIntervalDays, templateMarkdown, createdAt, updatedAt)
      - `OneOnOneEntry` (id, personId, meetingDate, agendaItems, notesMarkdown, outcomesMarkdown, sensitive, createdAt, updatedAt)
      - `AgendaItem` (id, text, checked, displayOrder, createdAt)
      - `CadenceType` enum (WEEKLY, BIWEEKLY, MONTHLY, CUSTOM)
    - _Requirements: 12.1, 13.1_

  - [ ] 10.2 Implement API client functions
    - Add to `frontend/src/lib/api-client.ts`:
      - `upsertOneOnOneSeries(personId, data)`
      - `getOneOnOneSeries(personId)`
      - `createOneOnOneEntry(personId, data)`
      - `getOneOnOneEntry(personId, entryId)`
      - `updateOneOnOneEntry(personId, entryId, data)`
      - `deleteOneOnOneEntry(personId, entryId)`
      - `listOneOnOneEntries(personId, page?, size?)`
    - All requests include Authorization Bearer header
    - _Requirements: 11.1–11.7_

  - [ ] 10.3 Write API client unit tests
    - Test each 1:1 API function constructs correct request (URL, method, headers, body)
    - Test error handling maps API errors correctly
    - _Requirements: 11.1–11.7_

- [ ] 11. Frontend — Reusable components
  - [ ] 11.1 Implement MarkdownEditor component
    - Rich Markdown editing (not plain textarea) — use a library like react-markdown + textarea with preview, or a lightweight editor
    - Support both edit and preview modes
    - Accept value and onChange props
    - _Requirements: 13.1, 13.2_

  - [ ] 11.2 Implement AgendaItemList component
    - Display ordered list of agenda items with checkboxes
    - Add new item input, remove button per item
    - Emit changes to parent (add, remove, toggle check)
    - Validate non-blank text before adding
    - _Requirements: 13.3_

  - [ ] 11.3 Implement OneOnOneEntryCard component
    - Display entry summary: meeting date, notes preview (~100 chars), agenda count, sensitive badge
    - Clickable — navigates to entry detail/edit page
    - Visually distinguish sensitive entries (lock icon, muted text)
    - _Requirements: 12.2, 12.5_

  - [ ] 11.4 Implement OneOnOneTimeline component
    - Paginated list of OneOnOneEntryCard components
    - Reverse chronological order
    - Empty state with "Start 1:1" button
    - "Hide sensitive" toggle that collapses sensitive entry previews
    - _Requirements: 12.1, 12.3, 12.4, 12.5, 12.6_

  - [ ] 11.5 Implement OneOnOneEntryForm component
    - Date/time picker for meeting date
    - AgendaItemList integration
    - MarkdownEditor for notes and outcomes
    - SensitiveToggle for sensitive flag
    - Support create and edit modes
    - Validate meeting date presence before submission
    - _Requirements: 13.1–13.8_

  - [ ] 11.6 Implement SeriesConfigPanel component
    - Cadence type selector (Weekly, Biweekly, Monthly, Custom)
    - Custom interval input (shown only when Custom selected)
    - MarkdownEditor for template
    - Save button triggers API call
    - _Requirements: 14.1–14.5_

  - [ ] 11.7 Implement SensitiveToggle and SensitiveBadge components
    - SensitiveToggle: checkbox/switch for marking content sensitive
    - SensitiveBadge: visual indicator (lock icon) for sensitive entries
    - _Requirements: 8.4, 12.5_

  - [ ] 11.8 Write component unit tests
    - Test MarkdownEditor renders and captures content
    - Test AgendaItemList handles add/remove/check, validates blank text
    - Test OneOnOneEntryCard renders date, preview, sensitive badge
    - Test OneOnOneTimeline renders entries in order, handles empty state, hide-sensitive toggle
    - Test OneOnOneEntryForm validates meeting date, integrates sub-components
    - Test SeriesConfigPanel shows custom interval only for CUSTOM cadence
    - Test SensitiveToggle/Badge render correct states
    - _Requirements: 12.1–12.6, 13.1–13.8, 14.1–14.5_

- [ ] 12. Frontend — Pages
  - [ ] 12.1 Update Person Detail page — add 1:1s tab
    - Add "1:1s" tab to the Person detail page
    - Integrate OneOnOneTimeline component
    - Add "Start 1:1" button navigating to create entry page
    - Add gear/settings icon to access SeriesConfigPanel
    - Update at-a-glance panel to show last1on1Date from API
    - _Requirements: 12.1, 12.4, 9.1_

  - [ ] 12.2 Implement Create Entry page (/people/[id]/one-on-ones/new)
    - OneOnOneEntryForm in create mode
    - Prefill notes from series template if available
    - On success, navigate to entry detail or back to person page
    - _Requirements: 13.6, 13.7_

  - [ ] 12.3 Implement Entry Detail/Edit page (/people/[id]/one-on-ones/[entryId])
    - Fetch and display full entry
    - OneOnOneEntryForm in edit mode
    - Delete button with confirmation
    - _Requirements: 13.6, 14.4_

  - [ ] 12.4 Write page integration tests
    - Test Person Detail 1:1s tab renders timeline, handles empty state
    - Test Create Entry page submits form and navigates on success
    - Test Entry Detail page renders all fields, handles edit/delete
    - Test SeriesConfigPanel saves configuration
    - _Requirements: 12.1–12.6, 13.1–13.8, 14.1–14.5_

- [ ] 13. Final checkpoint — Ensure all tests pass
  - Run `cd api && ./gradlew test` for all backend tests
  - Run `cd frontend && npm test` for all frontend tests
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 14. Documentation updates
  - [ ] 14.1 Update README.md
    - Add 1:1 Entry Management to feature list
    - Document new API endpoints
    - Update any relevant configuration notes

  - [ ] 14.2 Update PROGRESS.md
    - Mark 1:1 Entry Management as completed
    - Update current status
    - Update next steps (Action Items becomes priority 1)
    - Update test coverage summary

## Notes

- All persistence tests use Testcontainers with real PostgreSQL (no H2)
- Security invariant: every query MUST be scoped by userId — tested at multiple layers
- Agenda items are managed as part of the entry aggregate (full list replacement on update)
- Template prefill is server-side logic to keep it centralized and testable
- The MarkdownEditor component should use a lightweight library — avoid heavy dependencies
- Person cascade: deleting a Person should cascade to their series and entries (handled by FK ON DELETE CASCADE)
- The at-a-glance `last1on1Date` is computed from actual entry data, not denormalized
