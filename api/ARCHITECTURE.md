# Architecture Guide — Hexagonal DDD

This document describes the **target package structure** for the CrewCaptain API backend and guides future refactoring of domain aggregates into sub-packages.

## Current Package Layout (After Phase 0+1)

```
com.peoplemanager/
├── domain/                              ← Pure domain models (no Spring/JPA)
│   ├── *.kt                             ← Entities, VOs, Enums, Data classes
│   └── service/                         ← Domain services (pure logic)
│       ├── CsvParser.kt
│       ├── MarkdownExportFormatter.kt
│       └── ReviewPacketFormatter.kt
│
├── application/                         ← Use-case orchestration
│   ├── *Service.kt                      ← Application services (use cases)
│   ├── commands/                        ← Command objects (write operations)
│   ├── queries/                         ← Query objects (read operations)
│   └── port/
│       ├── input/                       ← Input Ports (CommandPort, QueryPort interfaces)
│       └── output/                      ← Output Ports (Repository interfaces, AI, Encryption)
│
└── adapters/                            ← Infrastructure implementations
    ├── ai/                              ← LLM/AI external service adapter
    ├── auth/                            ← Security config, JWT, user provisioning
    ├── encryption/                      ← AES-GCM encryption adapter
    ├── metrics/                         ← Prometheus metrics adapter
    ├── persistence/                     ← JPA entities + repository adapters
    ├── scheduler/                       ← Notification scheduler
    └── web/                             ← REST controllers
        └── dto/                         ← Request/Response DTOs
```

## Target Sub-Package Structure (Future Phases)

Once domain aggregates are migrated to sub-packages:

```
com.peoplemanager/
├── domain/
│   ├── model/
│   │   ├── shared/                      ← ValueObjects.kt (IDs, shared enums)
│   │   ├── person/                      ← Person, PinnedRememberItem, StickyNoteColor
│   │   ├── oneonone/                    ← OneOnOneSeries, OneOnOneEntry, AgendaItem
│   │   ├── actionitem/                  ← ActionItem
│   │   ├── pdp/                         ← PdpGoal, PdpUpdate
│   │   ├── kudos/                       ← Kudos
│   │   ├── quicknote/                   ← QuickNote
│   │   ├── notification/                ← Notification
│   │   ├── workspace/                   ← Workspace
│   │   ├── strategy/                    ← StrategyGoal, StrategyGoalPdpGoalLink
│   │   ├── user/                        ← User, UserSettings
│   │   └── gamification/                ← GamificationStats, DashboardData
│   ├── service/                         ← Domain services
│   └── event/                           ← Domain events (future)
│
├── application/
│   ├── port/
│   │   ├── input/                       ← Input Ports (use case interfaces)
│   │   └── output/                      ← Output Ports (repository + external)
│   ├── usecase/                         ← Service implementations (future rename)
│   ├── commands/                        ← Command objects (DTO for write)
│   └── queries/                         ← Query objects (DTO for read)
│
└── adapters/                            ← (unchanged)
```

## Architecture Rules (Enforced by ArchUnit)

The following rules are programmatically enforced by `HexagonalArchitectureTest`:

| Rule | Description |
|------|-------------|
| Domain isolation | `domain` must NOT depend on `application` or `adapters` |
| Domain purity | `domain` must NOT depend on Spring or JPA |
| Application isolation | `application` must NOT depend on `adapters` |
| Input port purity | `port.input` must NOT depend on `port.output` |
| Port purity | Neither `port.input` nor `port.output` may depend on `adapters` |
| Domain service purity | `domain.service` must NOT depend on `application`, `adapters`, or Spring |
| Adapter isolation | `web` must NOT depend on `persistence`; `persistence` must NOT depend on `web` |
| No cycles | No circular dependencies between top-level packages |

## Migration Playbook: Moving an Aggregate to Sub-Packages

To move an aggregate (e.g., `ActionItem`) into `domain/model/actionitem/`:

### Step 1: Create the target package
```bash
mkdir -p src/main/kotlin/com/peoplemanager/domain/model/actionitem
```

### Step 2: Move the file
```bash
cp src/main/kotlin/com/peoplemanager/domain/ActionItem.kt \
   src/main/kotlin/com/peoplemanager/domain/model/actionitem/ActionItem.kt
```

### Step 3: Update the package declaration
```kotlin
// Old:
package com.peoplemanager.domain

// New:
package com.peoplemanager.domain.model.actionitem

import com.peoplemanager.domain.model.shared.*  // For IDs, enums
```

### Step 4: Update all imports
```bash
find src -name "*.kt" -exec sed -i \
  's/import com\.peoplemanager\.domain\.ActionItem/import com.peoplemanager.domain.model.actionitem.ActionItem/g' {} +
```

### Step 5: Handle same-package references
Files still in `com.peoplemanager.domain` that used `ActionItem` without an import now need:
```kotlin
import com.peoplemanager.domain.model.actionitem.ActionItem
```

### Step 6: Compile and test
```bash
./gradlew compileKotlin compileTestKotlin test
```

### Step 7: Delete old file
```bash
rm src/main/kotlin/com/peoplemanager/domain/ActionItem.kt
```

## Key Design Decisions

1. **`Page`/`Pageable` kept in output ports** — Spring Data's Page/Pageable are used in repository interfaces. While technically a framework leak, replacing them with custom abstractions adds complexity with no practical benefit for this project size.

2. **`@Service`/`@Transactional` kept on application services** — These are marker annotations that enable Spring's classpath scanning. Removing them requires explicit `@Bean` configuration with no architectural improvement.

3. **Repository interfaces in `application/port/output/`** — Not in `domain/` because they sometimes reference Spring's Page type. Keeping them in the application layer's output port section is consistent with hexagonal semantics (they define what the application needs from infrastructure).

4. **Flat domain for now, sub-packages as future work** — The domain layer has no framework leakage. Sub-packaging is purely organizational. The ArchUnit tests enforce boundaries regardless of physical package structure.
