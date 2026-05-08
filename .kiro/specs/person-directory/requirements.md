# Requirements Document

## Introduction

The Person Directory is the foundational feature of CrewCaptain. It enables managers to maintain a private, manager-owned directory of people they manage. Each manager has their own isolated set of Person records with profile information, morale tracking, and pinned reminder notes. The feature also includes automatic User (manager) provisioning on first authenticated request via OIDC, ensuring a seamless onboarding experience.

## Glossary

- **System**: The CrewCaptain backend application
- **Frontend**: The CrewCaptain Next.js web application
- **User**: A manager account in the system, auto-provisioned from OIDC claims on first login
- **Person**: An individual managed by a User; belongs to exactly one User
- **Morale_Status**: An enumeration of morale values: GREEN, YELLOW, RED, UNKNOWN
- **Pinned_Remember_Item**: A short text bullet pinned to a Person's profile for quick reference
- **Person_Repository**: The persistence port responsible for storing and retrieving Person records
- **User_Repository**: The persistence port responsible for storing and retrieving User records
- **Auth_Adapter**: The adapter responsible for extracting authenticated user identity from JWT tokens
- **API**: The REST API layer serving JSON responses at /api/v1/

## Requirements

### Requirement 1: User Auto-Provisioning

**User Story:** As a manager using CrewCaptain for the first time, I want my account to be automatically created when I authenticate, so that I can start using the application without manual registration.

#### Acceptance Criteria

1. WHEN an authenticated request is received with a valid JWT and no matching User record exists, THE System SHALL create a new User record with the OIDC subject, issuer, display name, and email extracted from the JWT claims
2. WHEN an authenticated request is received with a valid JWT and a matching User record already exists, THE System SHALL use the existing User record without creating a duplicate
3. THE User_Repository SHALL identify Users by the combination of oidcSubject and oidcIssuer fields
4. WHEN a User record is auto-provisioned, THE System SHALL make the User available for the current request without requiring a second authentication attempt
5. IF the JWT is missing required claims (subject or issuer), THEN THE System SHALL reject the request with a 401 Unauthorized response

### Requirement 2: Person Creation

**User Story:** As a manager, I want to add people to my directory, so that I can track information about the individuals I manage.

#### Acceptance Criteria

1. WHEN a valid create-person request is received, THE System SHALL create a new Person record belonging to the authenticated User
2. THE System SHALL require the name field to be non-empty on every Person record
3. THE System SHALL accept optional fields: preferred name, role/title, timezone, start date, email, and tags
4. WHEN a Person is created successfully, THE System SHALL return the complete Person record with a generated unique identifier
5. IF the name field is empty or missing, THEN THE System SHALL reject the request with a 400 Bad Request response and a descriptive error message
6. THE System SHALL initialize morale status to UNKNOWN for newly created Person records
7. THE System SHALL initialize the pinned remember items list as empty for newly created Person records

### Requirement 3: Person Retrieval

**User Story:** As a manager, I want to view the details of a person in my directory, so that I can review their profile information at a glance.

#### Acceptance Criteria

1. WHEN a get-person request is received with a valid person identifier, THE System SHALL return the complete Person record including all profile fields, morale status, morale note, and pinned remember items
2. THE Person_Repository SHALL enforce that Person retrieval queries include the authenticated User's identifier
3. IF a person identifier does not exist or belongs to a different User, THEN THE System SHALL return a 404 Not Found response
4. THE System SHALL include placeholder fields for at-a-glance panel data (last 1:1 date, open action items count, active PDP goals summary) with empty/null values until those features are implemented

### Requirement 4: Person Update

**User Story:** As a manager, I want to update a person's profile information, so that I can keep their details current as roles and circumstances change.

#### Acceptance Criteria

1. WHEN a valid update-person request is received, THE System SHALL update the specified Person record with the provided field values
2. THE System SHALL allow updating: name, preferred name, role/title, timezone, start date, email, and tags
3. THE System SHALL require the name field to remain non-empty after an update
4. IF the target Person does not exist or belongs to a different User, THEN THE System SHALL return a 404 Not Found response
5. IF the name field is set to empty or null in an update request, THEN THE System SHALL reject the request with a 400 Bad Request response
6. WHEN a Person is updated successfully, THE System SHALL return the complete updated Person record

### Requirement 5: Person Deletion

**User Story:** As a manager, I want to remove a person from my directory, so that I can keep my list relevant when someone leaves my team.

#### Acceptance Criteria

1. WHEN a delete-person request is received with a valid person identifier, THE System SHALL remove the Person record from the authenticated User's directory
2. THE Person_Repository SHALL enforce that deletion queries include the authenticated User's identifier
3. IF the target Person does not exist or belongs to a different User, THEN THE System SHALL return a 404 Not Found response
4. WHEN a Person is deleted successfully, THE System SHALL return a 204 No Content response

### Requirement 6: Person List with Pagination

**User Story:** As a manager, I want to see a paginated list of all people in my directory, so that I can browse my team efficiently even with many direct reports.

#### Acceptance Criteria

1. THE System SHALL return a paginated list of Person records belonging to the authenticated User
2. THE System SHALL support page and size query parameters with defaults of page=0 and size=20
3. THE System SHALL return pagination metadata including total elements, total pages, current page number, and page size
4. THE Person_Repository SHALL enforce that list queries include the authenticated User's identifier, returning only that User's Person records
5. THE System SHALL order the Person list alphabetically by name as the default sort order

### Requirement 7: Person List Filtering

**User Story:** As a manager, I want to filter my people list by tag or morale status, so that I can quickly find specific groups within my team.

#### Acceptance Criteria

1. WHEN a tag filter parameter is provided, THE System SHALL return only Person records that contain the specified tag
2. WHEN a morale status filter parameter is provided, THE System SHALL return only Person records matching the specified morale status value
3. WHEN both tag and morale status filters are provided, THE System SHALL return only Person records matching both criteria
4. THE System SHALL validate that morale status filter values are one of GREEN, YELLOW, RED, or UNKNOWN
5. IF an invalid morale status filter value is provided, THEN THE System SHALL reject the request with a 400 Bad Request response

### Requirement 8: Morale Status Management

**User Story:** As a manager, I want to set and update a morale flag for each person, so that I can track how my team members are doing at a glance.

#### Acceptance Criteria

1. WHEN a set-morale request is received, THE System SHALL update the Person's morale status to the specified value
2. THE System SHALL restrict morale status values to the enumeration: GREEN, YELLOW, RED, UNKNOWN
3. THE System SHALL accept an optional morale note (free-text) alongside the morale status
4. IF an invalid morale status value is provided, THEN THE System SHALL reject the request with a 400 Bad Request response
5. IF the target Person does not exist or belongs to a different User, THEN THE System SHALL return a 404 Not Found response
6. WHEN morale status is updated successfully, THE System SHALL return the updated Person record

### Requirement 9: Pinned Remember Items Management

**User Story:** As a manager, I want to pin short reminder bullets to a person's profile, so that I can quickly recall important context before meetings.

#### Acceptance Criteria

1. WHEN an add-remember-item request is received with non-empty text, THE System SHALL append the item to the Person's pinned remember items list
2. WHEN a remove-remember-item request is received with a valid item identifier, THE System SHALL remove the specified item from the Person's pinned remember items list
3. WHEN a reorder-remember-items request is received with a new ordering, THE System SHALL update the display order of the Person's pinned remember items
4. THE System SHALL preserve the insertion order of pinned remember items unless explicitly reordered
5. IF the target Person does not exist or belongs to a different User, THEN THE System SHALL return a 404 Not Found response
6. IF the remember item text is empty, THEN THE System SHALL reject the request with a 400 Bad Request response
7. WHEN pinned remember items are modified successfully, THE System SHALL return the updated list of pinned remember items

### Requirement 10: Database Schema

**User Story:** As a developer, I want the database schema to be managed via Flyway migrations, so that schema changes are versioned, repeatable, and testable.

#### Acceptance Criteria

1. THE System SHALL provide a Flyway migration creating the users table with columns: id (UUID primary key), oidc_subject, oidc_issuer, display_name, email, created_at, updated_at
2. THE System SHALL provide a Flyway migration creating the persons table with columns: id (UUID primary key), user_id (foreign key to users), name, preferred_name, role_title, timezone, start_date, email, morale_status, morale_note, created_at, updated_at
3. THE System SHALL provide a Flyway migration creating the pinned_remember_items table with columns: id (UUID primary key), person_id (foreign key to persons), text, display_order, created_at
4. THE System SHALL create a unique index on users(oidc_subject, oidc_issuer) to prevent duplicate User records
5. THE System SHALL create an index on persons(user_id) to optimize queries scoped by User
6. THE System SHALL store tags as a PostgreSQL array column or a normalized join table on the persons table

### Requirement 11: REST API Endpoints

**User Story:** As a frontend developer, I want well-defined REST API endpoints, so that I can build the UI against a consistent and predictable interface.

#### Acceptance Criteria

1. THE API SHALL expose POST /api/v1/persons for creating a Person
2. THE API SHALL expose GET /api/v1/persons/{id} for retrieving a single Person
3. THE API SHALL expose PUT /api/v1/persons/{id} for updating a Person
4. THE API SHALL expose DELETE /api/v1/persons/{id} for deleting a Person
5. THE API SHALL expose GET /api/v1/persons for listing Persons with pagination and optional filters
6. THE API SHALL expose PUT /api/v1/persons/{id}/morale for updating a Person's morale status
7. THE API SHALL expose POST /api/v1/persons/{id}/remember-items for adding a pinned remember item
8. THE API SHALL expose DELETE /api/v1/persons/{id}/remember-items/{itemId} for removing a pinned remember item
9. THE API SHALL expose PUT /api/v1/persons/{id}/remember-items/reorder for reordering pinned remember items
10. THE API SHALL require a valid JWT Bearer token on all endpoints, returning 401 Unauthorized for unauthenticated requests
11. THE API SHALL return error responses in the format: { status, error, message, timestamp }

### Requirement 12: Authentication and Authorization

**User Story:** As a manager, I want all my data to be protected by authentication, so that only I can access my people directory.

#### Acceptance Criteria

1. THE System SHALL validate JWT tokens on every API request using the configured OIDC issuer and JWKS endpoint
2. IF a request lacks a valid JWT token, THEN THE System SHALL return a 401 Unauthorized response
3. THE System SHALL extract the authenticated User's identity from the JWT subject and issuer claims
4. THE System SHALL enforce that all Person operations are scoped to the authenticated User's records
5. IF a manager requests a resource belonging to a different manager, THEN THE System SHALL return a 404 Not Found response without revealing the resource exists

### Requirement 13: Frontend People List Page

**User Story:** As a manager, I want a people list page in the web application, so that I can see all my team members at a glance and navigate to their profiles.

#### Acceptance Criteria

1. THE Frontend SHALL display a paginated list of Person records retrieved from the API
2. THE Frontend SHALL show each Person's name, role/title, and morale status indicator in the list view
3. THE Frontend SHALL provide filter controls for tag and morale status
4. THE Frontend SHALL use consistent morale color coding: GREEN displays as green, YELLOW displays as amber, RED displays as red, UNKNOWN displays as gray
5. WHEN the people list is empty, THE Frontend SHALL display a helpful empty state message guiding the user to add their first person
6. THE Frontend SHALL provide a button or action to navigate to the create-person form
7. WHEN a Person list item is clicked, THE Frontend SHALL navigate to that Person's detail page

### Requirement 14: Frontend Person Detail Page

**User Story:** As a manager, I want a person detail page, so that I can view and edit all information about a specific team member.

#### Acceptance Criteria

1. THE Frontend SHALL display all Person profile fields: name, preferred name, role/title, timezone, start date, email, and tags
2. THE Frontend SHALL display the morale status with color-coded indicator and morale note
3. THE Frontend SHALL display the list of pinned remember items in their display order
4. THE Frontend SHALL provide edit functionality for profile fields
5. THE Frontend SHALL provide controls to set/update morale status and note
6. THE Frontend SHALL provide controls to add, remove, and reorder pinned remember items
7. THE Frontend SHALL display placeholder sections for at-a-glance panel data (last 1:1, action items, PDP goals) with empty state messaging indicating those features are coming soon
8. THE Frontend SHALL forward the access token as Authorization Bearer header on all API calls
