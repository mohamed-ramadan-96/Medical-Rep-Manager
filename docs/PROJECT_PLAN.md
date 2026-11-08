# Project plan

## Phase 1 — Foundation
- [x] Android application scaffold
- [x] Kotlin + Jetpack Compose + Material 3
- [x] Arabic / English in-app language switch
- [x] RTL layout for Arabic
- [x] GitHub Actions debug APK build

## Phase 2 — Local data
- [x] Room database foundation (versioned local database)
- [x] Doctor profiles: create, edit, delete
- [x] Multiple phone numbers, specialty, center/clinic text, and notes
- [x] Doctor search by name, specialty, phone, and center
- [x] Structured medical centers with address, phone, and notes
- [x] Doctor-to-center relationship with legacy center-name migration
- [ ] Doctor photos and document attachments
- [ ] Database migrations and data validation tests

## Phase 3 — Daily workflow
- [ ] Multiple weekly availability slots per doctor
- [ ] Visit records and follow-up notes
- [ ] Search and filters
- [ ] Dashboard statistics

## Phase 4 — Backup
- [ ] Google Sign-In / OAuth configuration
- [ ] Google Drive App Data folder backup
- [ ] Backup validation and version history
- [ ] Restore flow and conflict safeguards

## Product principles
- Offline-first; core workflows must work without internet.
- User owns the data and can export it.
- No mandatory paid backend or subscription.
- Never upload doctor data unless the user explicitly configures backup.
- Keep credentials and personal data out of Git.
