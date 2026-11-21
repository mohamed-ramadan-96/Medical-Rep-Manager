# Project plan

## Foundation
- [x] Android scaffold, Kotlin, Jetpack Compose, Material 3
- [x] Arabic / English interface and RTL layout
- [x] Persist language selection across process recreation
- [x] GitHub Actions signed release APK build

## Local data and workflows
- [x] Room database (schema version 12)
- [x] Doctor profiles, multiple phones, specialties, medical centers
- [x] Multiple doctor-to-center relationships
- [x] Photos and private document attachments
- [x] Weekly availability slots linked to doctors and centers
- [x] Visit history, outcomes, notes, and follow-up dates
- [x] Dashboard statistics, recent activity, upcoming follow-ups
- [x] Excel import/export for doctor names and phone numbers
- [x] GPS/map links and shared Google Maps URL handling
- [ ] Automated Room migration and DAO regression tests

## Backup and recovery
- [x] Optional Google Drive authorization and backup
- [x] Scheduled backups through WorkManager
- [x] Local ZIP export and restore
- [x] Database integrity and foreign-key checks
- [ ] End-to-end backup/restore tests on physical devices
- [ ] Encryption for exported backup archives
- [ ] Configurable Google Drive retention policy

## Engineering follow-ups
- [ ] Add Gradle wrapper and reproducible local build instructions
- [ ] Add Room schema export and migration fixtures
- [ ] Add CI test and lint jobs
- [ ] Review map URL redirect allowlists
- [ ] Add dedicated launcher and adaptive icon assets
- [ ] Add local notifications for upcoming follow-ups

## Product principles
- Offline-first; core workflows must work without internet.
- User owns the data and can export it.
- Google Drive backup is optional and user-authorized.
- Never upload doctor data unless the user explicitly configures backup.
- Keep credentials, signing keys, and personal data out of Git.

## Weekly visit planning
- [x] Reuse the existing doctor geographic area field (`area`) to build region choices; do not add a duplicate region field.
- [x] Monthly calendar-based weekly visit planning, grouped by region, center, and doctor.
- [x] Allow multiple doctors per center/day while preventing a doctor from being assigned more than once in a Saturday–Friday plan week.
- [x] Keep centers selectable across days; removing a doctor restores their availability for the rest of the week.
- [x] Export a selected week or full month to an offline Excel workbook.
- [x] Add Room migration 13→14 for weekly plan entries.
