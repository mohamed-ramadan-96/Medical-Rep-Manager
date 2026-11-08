# Medical Rep Manager

An offline-first Android application for medical representatives to manage doctors, clinics, medical centers, schedules, visit history, notes, and attachments.

## Project status

**Foundation / Phase 1** — Android project scaffold and bilingual Material 3 interface.

## Technology

- Kotlin
- Jetpack Compose + Material 3
- AndroidX Navigation Compose
- Room (planned for the local database phase)
- Google Drive API (planned for optional backup and restore)

## Languages

Arabic and English, with an in-app language switch and correct RTL layout for Arabic.

## Build

The GitHub Actions workflow builds a debug APK. Download it from the workflow run's **Artifacts** section.

The project uses Gradle's command-line build in CI; no Gradle wrapper binary is committed in this initial scaffold.

## Privacy

Doctor and clinic data is intended to remain local to the device unless the user explicitly configures backup. Never commit personal or medical contact data, credentials, OAuth secrets, or backup files to this repository.

## License

Apache License 2.0. See [LICENSE](LICENSE).
