# Implementation Plan: Person Directory

## Overview

This plan implements the Person Directory feature for CrewCaptain — the foundational feature providing managers with a private, isolated directory of people they manage. Implementation follows the hexagonal/DDD architecture with strict data isolation, progressing from database schema through domain, application, adapters, and finally the frontend.

## Tasks

- [x] 1. Database migrations
  - [x] 1.1 Create Flyway migration for users table
    - Create `api/src/main/resources/db/migration/V20250508120000__create_users_table.sql`
    - Define columns: id (UUID PK), oidc_subject, oidc_issuer, display_name, email, created_at, updated_at
    - Add unique constraint on (oidc_subject, oidc_issuer)
    - _Requirements: 10.1, 10.4_

  - [x] 1.2 Create Flyway migration for persons table
    - Create `api/src/main/resources/db/migration/V20250508120001__create_persons_table.sql`
    - Define columns: id (UUID PK), user_id (FK to users), name, preferred_name, role_title, timezone, start_date, email, tags (TEXT[]), morale_status, morale_note, created_at, updated_at
    - Add index on persons(user_id) and composite index on persons(user_id, morale_status)
    - _Requirements: 10.2, 10.5, 10.6_

  - [x] 1.3 Create Flyway migration for pinned_remember_items table
    - Create `api/src/main/resources/db/migration/V20250508120002__create_pinned_remember_items_table.sql`
    - Define columns: id (UUID PK), person_id (FK to persons with CASCADE delete), text, display_order, created_at
    - Add index on pinned_remember_items(person_id)
    - _Requirements: 10.3_

  - [x] 1.4 Write Flyway migration integration test
    - Create `FlywayMigrationTest.kt` in integration test package
    - Verify all migrations apply cleanly against Testcontainers PostgreSQL
    - Verify table structures match expected schema
    - _Requirements: 10.1, 10.2, 10.3_

- [x] 2. Domain layer — Value Objects and Aggregates
  - [x] 2.1 Implement Value Objects
    - Create `UserId`, `PersonId`, `RememberItemId` value classes wrapping UUID
    - Create `MoraleStatus` enum with GREEN, YELLOW, RED, UNKNOWN values
    - Create `OidcIdentity` value object with subject and issuer fields (both non-empty)
    - Place in `com.peoplemanager.domain` package
    - _Requirements: 8.2, 1.3_

  - [x] 2.2 Implement Person aggregate root
    - Create `Person` data class with all fields per design (id, userId, name, preferredName, roleTitle, timezone, startDate, email, tags, moraleStatus, moraleNote, pinnedRememberItems, createdAt, updatedAt)
    - Add `require(name.isNotBlank())` invariant in init block
    - Implement `updateMorale(status, note)` method
    - Implement `addRememberItem(text)`, `removeRememberItem(itemId)`, `reorderRememberItems(orderedIds)` methods
    - _Requirements: 2.2, 8.1, 8.2, 9.1, 9.2, 9.3_

  - [x] 2.3 Implement PinnedRememberItem entity
    - Create `PinnedRememberItem` data class with id, text, displayOrder, createdAt
    - Add `require(text.isNotBlank())` invariant
    - _Requirements: 9.1, 9.6_

  - [x] 2.4 Implement User aggregate root
    - Create `User` data class with id, oidcSubject, oidcIssuer, displayName, email, createdAt, updatedAt
    - _Requirements: 1.1, 1.3_

  - [x] 2.5 Write domain unit tests for Person aggregate
    - Test name-not-blank invariant (blank/whitespace strings rejected)
    - Test morale update returns new Person with updated status and note
    - Test addRememberItem appends to list with correct order
    - Test removeRememberItem removes correct item, preserves others
    - Test reorderRememberItems produces correct permutation
    - _Requirements: 2.2, 8.1, 9.1, 9.2, 9.3_

  - [x] 2.6 Write property test: Blank name rejection (Property 3)
    - **Property 3: Blank name rejection**
    - Generate arbitrary whitespace-only strings; verify Person construction throws IllegalArgumentException
    - Minimum 100 iterations
    - **Validates: Requirements 2.2, 2.5, 4.3, 4.5**

  - [x] 2.7 Write property test: Remember item addition preserves order and content (Property 11)
    - **Property 11: Remember item addition preserves order and content**
    - Generate arbitrary sequences of non-blank strings; verify all items present in insertion order with matching text
    - Minimum 100 iterations
    - **Validates: Requirements 9.1, 9.4**

  - [x] 2.8 Write property test: Remember item removal (Property 12)
    - **Property 12: Remember item removal**
    - Generate a Person with N remember items, pick a random item to remove; verify N-1 items remain with correct relative order
    - Minimum 100 iterations
    - **Validates: Requirements 9.2**

  - [x] 2.9 Write property test: Remember item reorder is a permutation (Property 13)
    - **Property 13: Remember item reorder is a permutation**
    - Generate a Person with remember items and a random permutation of IDs; verify items appear in new order with no additions/removals
    - Minimum 100 iterations
    - **Validates: Requirements 9.3**

- [x] 3. Application layer — Use Cases and Ports
  - [x] 3.1 Define port interfaces
    - Create `PersonRepository` interface (save, findByIdAndUserId, findAllByUserId, deleteByIdAndUserId)
    - Create `UserRepository` interface (findByOidcIdentity, save)
    - Place in `com.peoplemanager.application` package
    - All methods that return data MUST accept userId parameter
    - _Requirements: 3.2, 5.2, 6.4_

  - [x] 3.2 Define command and query data classes
    - Create `CreatePersonCommand`, `UpdatePersonCommand`, `DeletePersonCommand`, `SetMoraleCommand`
    - Create `AddRememberItemCommand`, `RemoveRememberItemCommand`, `ReorderRememberItemsCommand`
    - Create `GetPersonQuery`, `ListPersonsQuery`
    - _Requirements: 2.1, 4.1, 5.1, 6.1, 7.1, 8.1, 9.1_

  - [x] 3.3 Implement PersonCommandPort and PersonQueryPort interfaces
    - Define inbound port interfaces as specified in design
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7, 11.8, 11.9_

  - [x] 3.4 Implement CreatePersonUseCase
    - Accept CreatePersonCommand, create Person with UNKNOWN morale and empty remember items
    - Delegate to PersonRepository.save()
    - _Requirements: 2.1, 2.3, 2.6, 2.7_

  - [x] 3.5 Implement UpdatePersonUseCase
    - Load Person by id+userId, apply updates, save
    - Throw NotFoundException if not found (covers cross-user access)
    - _Requirements: 4.1, 4.2, 4.4, 4.6_

  - [x] 3.6 Implement DeletePersonUseCase
    - Delete by id+userId, throw NotFoundException if not found
    - _Requirements: 5.1, 5.2, 5.3_

  - [x] 3.7 Implement GetPersonUseCase and ListPersonsUseCase
    - GetPerson: find by id+userId, throw NotFoundException if not found
    - ListPersons: delegate to repository with pagination, tag filter, morale filter
    - Default sort: alphabetical by name
    - _Requirements: 3.1, 3.2, 3.3, 6.1, 6.2, 6.3, 6.5, 7.1, 7.2, 7.3_

  - [x] 3.8 Implement SetMoraleUseCase
    - Load Person, call updateMorale(), save
    - _Requirements: 8.1, 8.3, 8.5, 8.6_

  - [x] 3.9 Implement Remember Item use cases (Add, Remove, Reorder)
    - AddRememberItem: load Person, call addRememberItem(), save, return updated list
    - RemoveRememberItem: load Person, call removeRememberItem(), save, return updated list
    - ReorderRememberItems: load Person, call reorderRememberItems(), save, return updated list
    - _Requirements: 9.1, 9.2, 9.3, 9.5, 9.7_

  - [x] 3.10 Implement UserProvisioningService
    - Accept OIDC claims, look up User by OidcIdentity
    - If not found, create and save new User
    - Return existing or newly created User
    - _Requirements: 1.1, 1.2, 1.4_

  - [x] 3.11 Write use case unit tests with Mockk
    - Test CreatePersonUseCase: verify repository.save called with correct Person
    - Test UpdatePersonUseCase: verify not-found throws exception, valid update saves
    - Test DeletePersonUseCase: verify not-found throws, valid delete calls repository
    - Test GetPersonUseCase and ListPersonsUseCase: verify userId scoping
    - Test SetMoraleUseCase: verify morale update persisted
    - Test Remember Item use cases: verify add/remove/reorder logic
    - Test UserProvisioningService: verify idempotent provisioning
    - _Requirements: 2.1, 3.2, 4.4, 5.3, 6.4, 8.5, 9.5, 1.2_

  - [x] 3.12 Write property test: User provisioning idempotence (Property 1)
    - **Property 1: User provisioning idempotence**
    - Generate arbitrary OIDC claims; provision multiple times; verify same User returned, no duplicates
    - Minimum 100 iterations
    - **Validates: Requirements 1.1, 1.2, 1.3**

  - [x] 3.13 Write property test: Person creation round-trip (Property 2)
    - **Property 2: Person creation round-trip**
    - Generate valid CreatePersonCommands; create then retrieve; verify all fields match, moraleStatus=UNKNOWN, empty remember items
    - Minimum 100 iterations
    - **Validates: Requirements 2.1, 2.3, 2.4, 2.6, 2.7, 3.1**

  - [x] 3.14 Write property test: Person update preserves identity (Property 4)
    - **Property 4: Person update preserves identity and reflects changes**
    - Generate existing Person + valid update; verify same ID/userId, mutable fields reflect new values
    - Minimum 100 iterations
    - **Validates: Requirements 4.1, 4.2, 4.6**

  - [x] 3.15 Write property test: Morale status update round-trip (Property 10)
    - **Property 10: Morale status update round-trip**
    - Generate arbitrary MoraleStatus + optional note; set morale then retrieve; verify values match
    - Minimum 100 iterations
    - **Validates: Requirements 8.1, 8.2, 8.3, 8.6**

- [x] 4. Checkpoint — Ensure domain and application tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Persistence adapter
  - [x] 5.1 Implement JPA entities
    - Create `UserEntity` with JPA annotations mapping to users table
    - Create `PersonEntity` with JPA annotations mapping to persons table
    - Create `PinnedRememberItemEntity` mapping to pinned_remember_items table
    - Implement bidirectional mapping between domain objects and JPA entities
    - _Requirements: 10.1, 10.2, 10.3_

  - [x] 5.2 Implement JpaUserRepository
    - Implement `UserRepository` port interface
    - Query by oidc_subject + oidc_issuer combination
    - Map between User domain object and UserEntity
    - _Requirements: 1.3_

  - [x] 5.3 Implement JpaPersonRepository
    - Implement `PersonRepository` port interface
    - All queries MUST include user_id in WHERE clause
    - Implement pagination with Spring Data Pageable
    - Implement tag filter (array contains) and morale status filter
    - Default sort by name (case-insensitive)
    - Map between Person domain object and PersonEntity (including nested PinnedRememberItems)
    - _Requirements: 3.2, 5.2, 6.1, 6.4, 6.5, 7.1, 7.2, 7.3_

  - [x] 5.4 Write persistence integration tests with Testcontainers
    - Test UserRepository: save, findByOidcIdentity, unique constraint enforcement
    - Test PersonRepository: save, findByIdAndUserId, findAllByUserId with pagination
    - Test userId scoping: verify User A cannot access User B's persons
    - Test tag filtering and morale filtering
    - Test cascade delete (person deletion removes remember items)
    - Test default alphabetical sort order
    - _Requirements: 3.2, 6.4, 6.5, 7.1, 7.2, 10.4, 10.5_

- [x] 6. Auth adapter — JWT validation and user auto-provisioning
  - [x] 6.1 Implement JWT authentication filter
    - Create Spring Security configuration for OAuth2 resource server
    - Extract subject and issuer claims from validated JWT
    - Call UserProvisioningService to resolve/create User
    - Place resolved UserId into SecurityContext for downstream use
    - _Requirements: 1.1, 1.4, 1.5, 12.1, 12.3_

  - [x] 6.2 Create AuthenticatedUser helper
    - Create utility to extract UserId from SecurityContext in controllers
    - Ensure 401 is returned when no valid JWT is present
    - _Requirements: 12.1, 12.2_

  - [x] 6.3 Write auth adapter tests
    - Test valid JWT results in User provisioning and userId in context
    - Test missing JWT returns 401
    - Test invalid JWT returns 401
    - Test missing required claims (subject/issuer) returns 401
    - _Requirements: 1.5, 12.1, 12.2_

- [x] 7. Web adapter — REST controllers
  - [x] 7.1 Implement global exception handler
    - Create `@RestControllerAdvice` class
    - Map MethodArgumentNotValidException → 400 with standard error format
    - Map PersonNotFoundException → 404 with standard error format
    - Map generic exceptions → 500 with generic message (no internal details)
    - Return consistent JSON: { status, error, message, timestamp }
    - _Requirements: 11.11_

  - [x] 7.2 Implement PersonController — CRUD endpoints
    - POST /api/v1/persons — create person (201 Created)
    - GET /api/v1/persons/{id} — get person (200 OK)
    - PUT /api/v1/persons/{id} — update person (200 OK)
    - DELETE /api/v1/persons/{id} — delete person (204 No Content)
    - GET /api/v1/persons — list persons with pagination and filters (200 OK)
    - Extract userId from SecurityContext for all operations
    - Add @Valid on request DTOs with Bean Validation annotations
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5_

  - [x] 7.3 Implement PersonController — Morale and Remember Items endpoints
    - PUT /api/v1/persons/{id}/morale — set morale (200 OK)
    - POST /api/v1/persons/{id}/remember-items — add remember item (201 Created)
    - DELETE /api/v1/persons/{id}/remember-items/{itemId} — remove remember item (200 OK)
    - PUT /api/v1/persons/{id}/remember-items/reorder — reorder items (200 OK)
    - _Requirements: 11.6, 11.7, 11.8, 11.9_

  - [x] 7.4 Implement request/response DTOs
    - Create request DTOs with Bean Validation annotations (@NotBlank, @NotNull, etc.)
    - Create response DTOs matching the API contract in design document
    - Include at-a-glance placeholder fields (null values for now)
    - _Requirements: 2.3, 2.5, 3.4, 4.3, 7.4, 7.5, 8.4, 9.6_

  - [x] 7.5 Write controller slice tests (@WebMvcTest)
    - Test all CRUD endpoints with MockMvc (happy path + error cases)
    - Test validation: blank name returns 400, invalid morale returns 400
    - Test authentication required: requests without JWT return 401
    - Test not-found scenarios return 404
    - Test response format matches API contract
    - _Requirements: 11.1–11.11, 12.1, 12.2_

  - [x] 7.6 Write property test: Authentication required on all endpoints (Property 15)
    - **Property 15: Authentication required on all endpoints**
    - Generate arbitrary API paths under /api/v1/; verify unauthenticated requests return 401
    - Minimum 100 iterations
    - **Validates: Requirements 11.10, 12.1, 12.2**

  - [x] 7.7 Write property test: Error response format consistency (Property 16)
    - **Property 16: Error response format consistency**
    - Generate requests that produce errors (invalid input, not found, etc.); verify response contains status, error, message, timestamp fields
    - Minimum 100 iterations
    - **Validates: Requirements 11.11**

- [x] 8. Checkpoint — Ensure all backend tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. Integration tests — Full stack with security
  - [x] 9.1 Write data isolation integration test (Property 6)
    - **Property 6: Data isolation across users**
    - Create two users with persons; verify User B cannot access User A's persons via GET, PUT, DELETE, morale, remember-items
    - Verify list endpoint returns only authenticated user's persons
    - Cross-user access returns 404 (not 403)
    - Use Testcontainers + full Spring context
    - **Validates: Requirements 3.2, 3.3, 4.4, 5.2, 5.3, 6.4, 8.5, 9.5, 12.4, 12.5**

  - [x] 9.2 Write pagination and filtering integration test
    - **Property 7: Pagination metadata correctness**
    - Create N persons, verify totalElements=N, totalPages=ceil(N/size), content size correct
    - **Property 8: Default alphabetical sort order**
    - Verify persons returned alphabetically by name
    - **Property 9: Filter correctness**
    - Verify tag filter, morale filter, and combined filter return correct subsets
    - **Validates: Requirements 6.1, 6.2, 6.3, 6.5, 7.1, 7.2, 7.3**

  - [x] 9.3 Write delete-then-retrieve integration test (Property 5)
    - **Property 5: Delete then retrieval returns not found**
    - Create a person, delete it, verify GET returns 404
    - Use Testcontainers + full Spring context
    - **Validates: Requirements 5.1, 5.4**

  - [x] 9.4 Write property test: Invalid morale status rejection (Property 14)
    - **Property 14: Invalid morale status rejection**
    - Generate arbitrary strings not in {GREEN, YELLOW, RED, UNKNOWN}; verify 400 response
    - Minimum 100 iterations
    - **Validates: Requirements 7.4, 7.5, 8.4**

- [x] 10. Checkpoint — Ensure all backend tests pass including integration
  - Ensure all tests pass, ask the user if questions arise.

- [x] 11. Add Kotest property testing dependency
  - Add `io.kotest:kotest-property:5.8.1` to `api/build.gradle.kts` testImplementation dependencies
  - Verify build still compiles
  - _Requirements: (supports all property tests)_

- [x] 12. Frontend — Types and API client
  - [x] 12.1 Define TypeScript types
    - Create `frontend/src/types/person.ts` with Person, PinnedRememberItem, MoraleStatus, PaginatedResponse types
    - Create `frontend/src/types/api.ts` with ApiError type
    - All types must match the API response contract from design
    - _Requirements: 13.1, 14.1_

  - [x] 12.2 Implement API client
    - Create `frontend/src/lib/api-client.ts`
    - Implement functions: createPerson, getPerson, updatePerson, deletePerson, listPersons
    - Implement functions: setMorale, addRememberItem, removeRememberItem, reorderRememberItems
    - All requests include Authorization Bearer header from session
    - Handle error responses and map to typed errors
    - _Requirements: 11.1–11.9, 14.8_

  - [x] 12.3 Write API client unit tests
    - Test each API function constructs correct request (URL, method, headers, body)
    - Test error handling maps API errors correctly
    - _Requirements: 11.1–11.9_

- [x] 13. Frontend — Reusable components
  - [x] 13.1 Implement MoraleIndicator component
    - Display color-coded morale badge: GREEN=green, YELLOW=amber, RED=red, UNKNOWN=gray
    - Accept moraleStatus prop
    - _Requirements: 13.4, 14.2_

  - [x] 13.2 Implement PersonCard component
    - Display person name, role/title, and MoraleIndicator
    - Clickable — navigates to person detail page
    - _Requirements: 13.2, 13.7_

  - [x] 13.3 Implement FilterBar component
    - Tag filter input and morale status dropdown
    - Emit filter change events to parent
    - _Requirements: 13.3_

  - [x] 13.4 Implement PersonForm component
    - Form fields for all Person profile fields (name required, others optional)
    - Client-side validation (name not blank)
    - Support create and edit modes
    - _Requirements: 14.1, 14.4_

  - [x] 13.5 Implement RememberItemsList component
    - Display pinned remember items in order
    - Add new item input, remove button per item, drag-to-reorder
    - _Requirements: 14.3, 14.6_

  - [x] 13.6 Implement EmptyState component
    - Helpful message when list is empty
    - Call-to-action button to add first person
    - _Requirements: 13.5_

  - [x] 13.7 Implement Pagination component
    - Page navigation controls
    - Display current page / total pages
    - _Requirements: 13.1_

  - [x] 13.8 Write component unit tests
    - Test MoraleIndicator renders correct colors for each status
    - Test PersonCard renders name, role, morale indicator
    - Test FilterBar emits correct filter values
    - Test PersonForm validates required name field
    - Test RememberItemsList renders items in order, handles add/remove
    - Test EmptyState renders message and CTA
    - Test Pagination renders correct page info
    - _Requirements: 13.1–13.7, 14.1–14.6_

- [x] 14. Frontend — Pages
  - [x] 14.1 Implement People List page (/people)
    - Fetch and display paginated person list from API
    - Integrate FilterBar, PersonCard, Pagination, EmptyState components
    - Add "Add Person" button navigating to /people/new
    - Wrap in session guard (authenticated only)
    - _Requirements: 13.1, 13.2, 13.3, 13.5, 13.6, 13.7_

  - [x] 14.2 Implement Person Detail page (/people/[id])
    - Fetch and display full person profile
    - Integrate PersonForm (edit mode), MoraleIndicator, RememberItemsList
    - Morale update controls (status dropdown + note field)
    - Placeholder sections for at-a-glance panel (1:1, action items, PDP goals)
    - Delete person with confirmation
    - _Requirements: 14.1, 14.2, 14.3, 14.4, 14.5, 14.6, 14.7_

  - [x] 14.3 Implement Create Person page (/people/new)
    - PersonForm in create mode
    - On success, navigate to person detail page
    - _Requirements: 13.6_

  - [x] 14.4 Write page integration tests
    - Test People List page renders persons, handles empty state, pagination
    - Test Person Detail page renders all fields, handles edit/delete
    - Test Create Person page submits form and navigates on success
    - _Requirements: 13.1–13.7, 14.1–14.8_

- [x] 15. Final checkpoint — Ensure all tests pass
  - Run `cd api && ./gradlew test` for all backend tests
  - Run `cd frontend && npm test` for all frontend tests
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- Task 11 (Kotest property dependency) should be done before running property tests — move it earlier if implementing property tests inline
- All persistence tests use Testcontainers with real PostgreSQL (no H2)
- Security invariant: every query MUST be scoped by userId — this is tested at multiple layers
