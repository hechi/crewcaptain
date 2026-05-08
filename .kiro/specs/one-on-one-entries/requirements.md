# Requirements Document: 1:1 Entry Management

## Introduction

The 1:1 Entry Management feature is the core value driver of CrewCaptain. It enables managers to configure recurring 1:1 cadences per person, create structured meeting entries with Markdown notes, track agenda items, record outcomes, and create action items inline. Each 1:1 series and entry is strictly scoped to the owning manager, maintaining full data isolation. The feature also supports per-person templates to prefill new entries and a sensitive content flag for private notes.

## Glossary

- **System**: The CrewCaptain backend application
- **Frontend**: The CrewCaptain Next.js web application
- **User**: A manager account in the system (auto-provisioned from OIDC)
- **Person**: An individual managed by a User; belongs to exactly one User
- **OneOnOneSeries**: The cadence configuration and template for a manager's recurring 1:1s with a specific Person
- **OneOnOneEntry**: A single 1:1 meeting record containing agenda, notes, outcomes, and metadata
- **Cadence**: The recurring frequency of 1:1 meetings (weekly, biweekly, monthly, custom)
- **AgendaItem**: A checkbox-style bullet point within a 1:1 entry
- **Sensitive**: A boolean flag indicating content should be treated with extra privacy care

## Requirements

### Requirement 1: 1:1 Series Configuration

**User Story:** As a manager, I want to configure a recurring 1:1 cadence for each person I manage, so that the system can track when my next 1:1 is due and alert me if I fall behind.

#### Acceptance Criteria

1. WHEN a manager creates or updates a 1:1 series for a Person, THE System SHALL store the cadence type (WEEKLY, BIWEEKLY, MONTHLY, CUSTOM) and optional custom interval in days
2. THE System SHALL allow at most one OneOnOneSeries per (User, Person) combination
3. WHEN no series exists for a Person, THE System SHALL treat the cadence as unconfigured (no reminders generated)
4. THE System SHALL allow the manager to set a Markdown template string on the series, which will prefill new 1:1 entries
5. THE System SHALL validate that custom interval is a positive integer when cadence type is CUSTOM
6. IF the target Person does not exist or belongs to a different User, THEN THE System SHALL return a 404 Not Found response
7. WHEN a series is created or updated successfully, THE System SHALL return the complete series record

### Requirement 2: 1:1 Entry Creation

**User Story:** As a manager, I want to quickly create a 1:1 entry for a person, so that I can capture notes during or after our meeting.

#### Acceptance Criteria

1. WHEN a valid create-entry request is received, THE System SHALL create a new OneOnOneEntry belonging to the authenticated User and the specified Person
2. THE System SHALL require the meetingDate field (defaults to current timestamp if not provided by client)
3. THE System SHALL accept optional fields: agenda items (list), notes (Markdown), outcomes (Markdown), and sensitive flag
4. WHEN a 1:1 series with a template exists for the Person, THE System SHALL prefill the notes field with the template content if notes are not provided in the request
5. THE System SHALL initialize the sensitive flag to false if not provided
6. WHEN a 1:1 entry is created successfully, THE System SHALL return the complete entry record with a generated unique identifier
7. IF the target Person does not exist or belongs to a different User, THEN THE System SHALL return a 404 Not Found response

### Requirement 3: 1:1 Entry Retrieval

**User Story:** As a manager, I want to view a specific 1:1 entry, so that I can review what was discussed and decided.

#### Acceptance Criteria

1. WHEN a get-entry request is received with a valid entry identifier, THE System SHALL return the complete OneOnOneEntry including all fields
2. THE System SHALL enforce that entry retrieval queries include the authenticated User's identifier
3. IF an entry identifier does not exist or belongs to a different User, THEN THE System SHALL return a 404 Not Found response

### Requirement 4: 1:1 Entry Update

**User Story:** As a manager, I want to update a 1:1 entry after the meeting, so that I can add notes, mark agenda items complete, and record outcomes.

#### Acceptance Criteria

1. WHEN a valid update-entry request is received, THE System SHALL update the specified OneOnOneEntry with the provided field values
2. THE System SHALL allow updating: meetingDate, agenda items, notes, outcomes, and sensitive flag
3. IF the target entry does not exist or belongs to a different User, THEN THE System SHALL return a 404 Not Found response
4. WHEN an entry is updated successfully, THE System SHALL return the complete updated entry record

### Requirement 5: 1:1 Entry Deletion

**User Story:** As a manager, I want to delete a 1:1 entry, so that I can remove entries created by mistake.

#### Acceptance Criteria

1. WHEN a delete-entry request is received with a valid entry identifier, THE System SHALL remove the OneOnOneEntry
2. THE System SHALL enforce that deletion queries include the authenticated User's identifier
3. IF the target entry does not exist or belongs to a different User, THEN THE System SHALL return a 404 Not Found response
4. WHEN an entry is deleted successfully, THE System SHALL return a 204 No Content response

### Requirement 6: 1:1 Entry List with Pagination

**User Story:** As a manager, I want to see a paginated list of all 1:1 entries for a person, so that I can browse our meeting history.

#### Acceptance Criteria

1. THE System SHALL return a paginated list of OneOnOneEntry records for a specific Person belonging to the authenticated User
2. THE System SHALL support page and size query parameters with defaults of page=0 and size=20
3. THE System SHALL return pagination metadata including total elements, total pages, current page number, and page size
4. THE System SHALL order entries by meetingDate descending (most recent first) as the default sort order
5. THE System SHALL enforce that list queries include the authenticated User's identifier
6. IF the target Person does not exist or belongs to a different User, THEN THE System SHALL return a 404 Not Found response

### Requirement 7: Agenda Items Management

**User Story:** As a manager, I want to add checkbox-style agenda items to a 1:1 entry, so that I can track discussion topics and mark them as covered.

#### Acceptance Criteria

1. THE System SHALL store agenda items as an ordered list within a OneOnOneEntry
2. Each agenda item SHALL have: text (required, non-empty), checked status (boolean, default false), and display order
3. WHEN an entry is created or updated, THE System SHALL accept a list of agenda items and persist them in the specified order
4. THE System SHALL allow toggling the checked status of individual agenda items via the entry update endpoint
5. IF an agenda item has empty text, THEN THE System SHALL reject the request with a 400 Bad Request response

### Requirement 8: Sensitive Content Flag

**User Story:** As a manager, I want to flag a 1:1 entry as sensitive, so that the content is clearly marked as private and can be hidden in the UI.

#### Acceptance Criteria

1. THE System SHALL store a sensitive boolean flag on each OneOnOneEntry (default: false)
2. WHEN the sensitive flag is true, THE System SHALL include a "sensitive": true field in the API response
3. THE System SHALL allow toggling the sensitive flag via the entry update endpoint
4. THE Frontend SHALL visually indicate sensitive entries with a distinct marker
5. THE Frontend SHALL support a "hide sensitive content" toggle that collapses sensitive entry details in list views

### Requirement 9: Person At-a-Glance — Last 1:1 Date

**User Story:** As a manager, I want to see when my last 1:1 with a person was, so that I can quickly assess if I'm overdue for a check-in.

#### Acceptance Criteria

1. WHEN retrieving a Person record, THE System SHALL include the most recent meetingDate from that Person's OneOnOneEntry records in the at-a-glance response
2. IF no 1:1 entries exist for the Person, THE System SHALL return null for the last1on1Date field
3. THE System SHALL compute this value from the actual entry data (not a cached/denormalized field)

### Requirement 10: Database Schema

**User Story:** As a developer, I want the 1:1 tables managed via Flyway migrations, so that schema changes are versioned and testable.

#### Acceptance Criteria

1. THE System SHALL provide a Flyway migration creating the one_on_one_series table with columns: id (UUID PK), user_id (FK to users), person_id (FK to persons), cadence_type (VARCHAR), custom_interval_days (INTEGER nullable), template_markdown (TEXT nullable), created_at, updated_at
2. THE System SHALL provide a Flyway migration creating the one_on_one_entries table with columns: id (UUID PK), user_id (FK to users), person_id (FK to persons), meeting_date (TIMESTAMP WITH TIME ZONE), notes_markdown (TEXT nullable), outcomes_markdown (TEXT nullable), sensitive (BOOLEAN default false), created_at, updated_at
3. THE System SHALL provide a Flyway migration creating the agenda_items table with columns: id (UUID PK), entry_id (FK to one_on_one_entries with CASCADE delete), text (TEXT NOT NULL), checked (BOOLEAN default false), display_order (INTEGER), created_at
4. THE System SHALL create a unique constraint on one_on_one_series(user_id, person_id)
5. THE System SHALL create indexes on one_on_one_entries(user_id, person_id) and one_on_one_entries(person_id, meeting_date DESC)
6. THE System SHALL create an index on agenda_items(entry_id)

### Requirement 11: REST API Endpoints

**User Story:** As a frontend developer, I want well-defined REST API endpoints for 1:1 management, so that I can build the UI against a consistent interface.

#### Acceptance Criteria

1. THE API SHALL expose PUT /api/v1/persons/{personId}/one-on-one-series for creating/updating the 1:1 series configuration
2. THE API SHALL expose GET /api/v1/persons/{personId}/one-on-one-series for retrieving the series configuration
3. THE API SHALL expose POST /api/v1/persons/{personId}/one-on-one-entries for creating a 1:1 entry
4. THE API SHALL expose GET /api/v1/persons/{personId}/one-on-one-entries/{entryId} for retrieving a single entry
5. THE API SHALL expose PUT /api/v1/persons/{personId}/one-on-one-entries/{entryId} for updating an entry
6. THE API SHALL expose DELETE /api/v1/persons/{personId}/one-on-one-entries/{entryId} for deleting an entry
7. THE API SHALL expose GET /api/v1/persons/{personId}/one-on-one-entries for listing entries with pagination
8. THE API SHALL require a valid JWT Bearer token on all endpoints, returning 401 Unauthorized for unauthenticated requests
9. THE API SHALL return error responses in the standard format: { status, error, message, timestamp }

### Requirement 12: Frontend — 1:1 Entry List (Person Timeline)

**User Story:** As a manager, I want to see a timeline of all 1:1s with a person on their detail page, so that I can browse our meeting history.

#### Acceptance Criteria

1. THE Frontend SHALL display a paginated list of 1:1 entries on the Person detail page under a "1:1s" tab
2. THE Frontend SHALL show each entry's meeting date, a preview of notes (first ~100 characters), agenda item count, and sensitive indicator
3. THE Frontend SHALL order entries by meeting date descending (most recent first)
4. WHEN the entry list is empty, THE Frontend SHALL display a helpful empty state with a "Start 1:1" button
5. THE Frontend SHALL visually distinguish sensitive entries (e.g., lock icon, muted preview)
6. WHEN "hide sensitive" toggle is active, THE Frontend SHALL collapse sensitive entry previews

### Requirement 13: Frontend — 1:1 Entry Editor

**User Story:** As a manager, I want a rich editor for 1:1 entries, so that I can capture structured notes with agenda items and Markdown formatting.

#### Acceptance Criteria

1. THE Frontend SHALL provide a Markdown editor for the notes field (not a plain textarea)
2. THE Frontend SHALL provide a Markdown editor for the outcomes field
3. THE Frontend SHALL provide an agenda items section with checkbox inputs and add/remove controls
4. THE Frontend SHALL provide a date/time picker for the meeting date
5. THE Frontend SHALL provide a toggle for the sensitive flag with a clear visual indicator
6. THE Frontend SHALL support both create and edit modes
7. WHEN creating a new entry and a series template exists, THE Frontend SHALL prefill the notes field with the template content
8. THE Frontend SHALL validate that at least the meeting date is present before submission

### Requirement 14: Frontend — 1:1 Series Configuration

**User Story:** As a manager, I want to configure the 1:1 cadence and template for a person, so that the system tracks my meeting rhythm.

#### Acceptance Criteria

1. THE Frontend SHALL provide a settings/configuration panel accessible from the Person detail page for 1:1 series
2. THE Frontend SHALL provide a cadence type selector (Weekly, Biweekly, Monthly, Custom)
3. WHEN Custom cadence is selected, THE Frontend SHALL show an input for custom interval in days
4. THE Frontend SHALL provide a Markdown editor for the template field
5. THE Frontend SHALL save the series configuration via the API on form submission
