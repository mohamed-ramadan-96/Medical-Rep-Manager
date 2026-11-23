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
- Stable release update checker: the app checks a separate public updates repository and offers an in-app APK download when a newer stable version is available.

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

The manual GitHub Actions workflow `.github/workflows/firebase-app-distribution.yml` has two build modes:

| Mode | Output |
|---|---|
| `tester` | Builds a signed APK and distributes it to the configured Firebase App Distribution tester group. Does not publish a public stable release. |
| `final` | Builds a signed APK, creates a GitHub Release in the separate public updates repository, and uploads the APK plus `latest.json`. |

### Signing secrets

Configure these repository secrets in the private source repository:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

### Firebase tester secrets

Required only for `tester` builds:

- `FIREBASE_SERVICE_ACCOUNT`
- `FIREBASE_APP_ID`
- `FIREBASE_GROUP`

### Public updates repository setup

The Android source repository stays private. Stable APKs and update metadata are published separately to:

`mohamed-ramadan-96/Medical-Rep-Manager-Releases`

Create this repository as **Public** and keep it dedicated to release assets only. Do not copy the Android source code into it.

Then create a GitHub Personal Access Token (fine-grained is preferred) with **Contents: Read and write** access limited to the updates repository. Add it as the source repository Actions secret:

`RELEASES_REPO_TOKEN`

The final workflow uses this token to publish the release in the public updates repository. The app checks:

`https://api.github.com/repos/mohamed-ramadan-96/Medical-Rep-Manager-Releases/releases/latest`

Each stable release must include:

- `MedicalRepManager-<version>.apk`
- `latest.json` with `versionName`, `versionCode`, `apkUrl`, and `releaseNotes`

The APK must be signed with the same signing key as the installed app. Always use a new, increasing `versionCode` and a new version tag (for example `v1.2.0`). The in-app update checker compares the manifest version code with the installed app version code.

### First installation of the updater

An app build that predates the in-app update checker cannot discover or install the updater retroactively. Install one newer signed build containing the update checker manually. After that, newer stable releases can be detected and downloaded from inside the app. Android still requires the user to confirm APK installation.

## Backup and privacy

Android system Auto Backup is disabled so doctor and clinic records are not silently copied outside the app's explicit backup flow. Google Drive backup is optional and requires user authorization. Backups contain the local database, doctor photos, and attachments; protect access to the Google account and downloaded backup files.

Never commit personal or medical contact data, credentials, OAuth secrets, keystores, or backup files to this repository. Never put the releases repository token in source code or in the public updates repository.

See [docs/PROJECT_PLAN.md](docs/PROJECT_PLAN.md) for implemented work and remaining validation tasks.

## License

Apache License 2.0. See [LICENSE](LICENSE).
