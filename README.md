# My Shelf v1.0

Your stories, all in one place. An offline Android tracker for movies, series, TV shows, comics, books, and more, built with Kotlin and Jetpack Compose.

No accounts, API keys, analytics, or automatic catalog requests. Internet access is used only when you explicitly import a cover from an HTTPS image link.

## Features

- **Startup:** a native logo splash with light/dark backgrounds and a short fade into Home or PIN unlock, without an artificial loading delay.
- **Home:** a doodle welcome banner, horizontal Continue and Recently added rows, and View all shortcuts when a row has titles. Empty rows show only their empty state. The + menu springs open to the left for Card or Collection creation. Home stays fixed vertically and adapts to short landscape windows.
- **My Shelf:** cover cards, search, media and status filters, sorting, and saved browsing positions. Choose 2, 3, or 4 cards per row and Comfortable or Compact details.
- **Collections:** names, descriptions, and 16 preset icons. Collections can span media types; titles keep their own genre labels. Create empty collections or group existing titles.
- **Title details:** ratings, year, creator, genre, notes, and immediate progress updates. Long-press for quick actions; removal supports Undo. Editing warns about unsaved changes and possible duplicates.
- **Seasons:** one card per series, with season names, ratings, statuses, watched counts, and optional episode totals. Completed seasons show Caught up until Series has ended is enabled.
- **Progress:** On my list, In progress, Caught up, Finished, On hold, and Dropped. Status filters appear on title grids; the collections list has Sort only.
- **Appearance:** System, Light, or Dark theme, six accents, and a shared animated navigation pill around the selected icon and label.
- **Privacy:** four-digit PIN, optional biometrics, and immediate background locking.
- **Backups:** portable ZIP import/export with covers, seasons, and collection customizations.

## Development

Open the project in Android Studio with Android SDK 35 and a JDK 17+ Gradle runtime. The app requires Android 8.0 (API 26) or newer. Configure `sdk.dir` in untracked `local.properties` if needed.

Run checks without packaging an APK:

```powershell
.\gradlew.bat compileDebugKotlin testDebugUnitTest lintDebug
```

Add `--offline` when dependencies are already cached. Initial setup may download Gradle and Android dependencies. Build APKs only when explicitly requested.

The release version is configured in `app/build.gradle.kts`: version name `1.0`, version code `2`. Settings' About section reads the version from `BuildConfig`.

## Covers

Tap a cover or doodle to choose a device image, paste a clipboard image, add a direct HTTPS image link, or remove the cover. Clipboard contents are read only after a paste action. Image links must point to an image rather than a web page.

New covers are validated, resized to at most 1,600 pixels on the longest edge, and stored as private JPEGs. Downloads are limited to 8 MB with timeouts and HTTPS-only redirects. Saved covers work offline and travel with backups. Older document-linked covers remain supported but depend on their source document remaining available.

## Backups

Use Import or Export in Settings. Import validates the ZIP and previews changes before applying them:

| Mode | Result |
| --- | --- |
| Skip existing | Add new title IDs and keep matching entries and collection customizations. |
| Update matching | Replace matching title metadata and covers, apply matching collection customizations, and keep unrelated titles. |
| Replace shelf | Restore the backup as the entire shelf after a second confirmation. |

Matching uses stable IDs. Collection renames follow their IDs; conflicting renames are rejected. Older backups without seasons or collection profiles remain supported. Cancelling leaves the shelf unchanged.

Limits: 2,000 titles, 8 MB per cover, 60 MB total cover data, and 4 MB metadata. Missing cover files are omitted and reported on export. Appearance and lock settings stay on the device.

Backups contain personal library data and are unencrypted. Export before uninstalling to keep your shelf. Personal exports and the local MCU generator/test are ignored by Git.

## App lock

Enable App Lock, enter four digits, then confirm them. Disabling requires the PIN. Biometric unlock requires PIN setup and a successful Android biometric prompt.

The app locks on cold launch and immediately when it moves to the background, including file pickers. Rotation preserves the current session. Screenshots and recent-app previews are blocked while lock is enabled. Five failed PIN attempts trigger a persisted 30-second cooldown.

A salted HMAC verifier uses a device-bound Android Keystore key. App lock controls access; it does not encrypt the shelf. There is no PIN recovery flow.

## Architecture

| Package | Responsibility |
| --- | --- |
| `core/model` | Shared models |
| `domain` | Framework-free rules |
| `data` | Local storage, covers, and backups |
| `features` | Screens and feature ViewModels |
| `navigation` | Routing, navigation controls, and browsing state |
| `state` | Shared shelf state |
| `ui` | Reusable components and theme |

`MainActivity` hosts Compose, lifecycle, and biometrics. Tests mirror the source hierarchy and check dependency boundaries. See the [architecture guide](docs/architecture.md).

The shelf starts empty. Each title supports one genre and one collection. There is no cloud sync, automatic metadata or season fetching, individual episode metadata, or chapter tracking. Uninstalling removes device-local data.

## Icon attribution

The Shelves navigation icon comes from [Google Material Symbols](https://github.com/google/material-design-icons), under the [Apache 2.0 license](docs/licenses/material-design-icons-LICENSE.txt).
