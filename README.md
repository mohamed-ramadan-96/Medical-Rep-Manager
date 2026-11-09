# Medical Rep Manager

An offline-first Android application for medical representatives to manage doctors, medical centers, specialties, availability schedules, visit history, follow-ups, locations, photos, and attachments.

## Current capabilities

- Arabic and English interface with RTL support and persisted language preference.
- Doctor profiles with multiple phone numbers, specialties, multiple medical-center links, notes, photos, map locations, and attachments.
- Medical centers and specialties management.
- Weekly availability slots linked to doctors and centers.
- Visit history, outcomes, notes, and follow-up dates.
- Dashboard statistics, recent activity, and upcoming follow-ups.
- Excel import/export for doctor names and phone numbers.
- Full local ZIP backup and optional Google Drive backup/restore, including database, doctor photos, and attachments.
- Scheduled Google Drive backups through WorkManager.

## Technology

- Kotlin 2.2.20, Jetpack Compose, Material 3
- Room 2.8.3 (database schema version 12)
- WorkManager, Google Identity authorization and Google Drive API
- Android location services and OpenStreetMap geocoding fallback
- Coil for image loading

## Android requirements

- Minimum SDK: 26; Target / Compile SDK: 36
- JDK 17; Gradle 8.13
- Gradle wrapper is not currently committed; CI provisions Gradle.

## Build and CI

GitHub Actions builds a signed release APK for pushes to `main` and `Final`, pull requests targeting `main`, and manual workflow dispatches. The APK is uploaded as the `medical-rep-manager-release` artifact.

Release signing requires repository secrets `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD`.

## Backup and privacy

Android system Auto Backup is disabled so doctor and clinic records are not silently copied outside the app's explicit backup flow. Google Drive backup is optional and requires user authorization. Backups contain the local database, doctor photos, and attachments; protect access to the Google account and downloaded backup files.

Never commit personal or medical contact data, credentials, OAuth secrets, keystores, or backup files to this repository.

See [docs/PROJECT_PLAN.md](docs/PROJECT_PLAN.md) for implemented work and remaining validation tasks.

## License

Apache License 2.0. See [LICENSE](LICENSE).
