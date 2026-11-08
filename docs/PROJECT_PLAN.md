# Project plan

## Phase 1 — Foundation
- [x] Android application scaffold
- [x] Kotlin + Jetpack Compose + Material 3
- [x] Arabic / English in-app language switch
- [x] RTL layout for Arabic
- [x] GitHub Actions debug APK build

## Phase 2 — Local data
- [ ] Room database and migrations
- [ ] Doctor profiles and multiple phone numbers
- [ ] Specialties
- [ ] Medical centers and doctor relationships
- [ ] Image and document attachments

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
