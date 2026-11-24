# Medical Rep Manager — Architecture Notes

## Current application shape

The app remains a single Android application module. Architecture is being improved incrementally so existing Room data, navigation, and user workflows remain stable while responsibilities move out of large Compose screens.

## Responsibility boundaries

- **UI (`ui/`)**: Compose screens render state, collect user input, and launch work in coroutines. They should not own parsing, normalization, or multi-step persistence rules.
- **Data (`data/`)**: Room entities/DAOs and services coordinate persistence. Multi-row operations that must succeed or fail together belong in a Room transaction here.
- **Domain (`domain/`)**: Pure business rules that do not depend on Android, Compose, or Room.

## Extracted domain rules

- `WeeklyPlanRules`: Saturday-based week calculation and geographic-area matching for weekly planning.
- `ContactPhoneRules`: phone splitting and normalization, including Arabic-Indic and Persian digits.

These functions are deterministic and are covered by instrumented tests so locale/device settings cannot silently change the rules.

## Data services

- `DoctorImportService`: performs duplicate detection and doctor inserts in one Room transaction. A failed import is rolled back rather than leaving a partial import.

## Performance conventions

- Use `remember(keys)` for derived Compose collections and indexes when their source values are unchanged.
- Build ID lookup maps once per source-list update instead of repeatedly scanning the full list inside nested filters.
- Keep expensive sorting, grouping, and aggregation out of frequently recomposed UI paths.

## Persistence and safety conventions

- Keep Room migrations explicit and sequential; do not use destructive migration fallback for user-owned data.
- Validate backup archives before replacing installed data. Reject traversal paths, duplicate entries, and oversized archives; validate SQLite integrity and foreign keys before installation.
- Bound imported workbook XML sizes before parsing.
- Keep backup restore rollback behavior and validate it with regression tests before changing its file-install sequence.

## Next architectural increments

1. Move remaining screen-owned multi-step workflows into focused data services.
2. Introduce repositories only where they create a stable boundary for multiple data sources or testable business behavior; avoid wrapper-only repositories.
3. Split oversized Compose screens by feature responsibility while preserving their current navigation and state contracts.
4. Add migration fixtures for supported historical schema versions before changing migration logic.
