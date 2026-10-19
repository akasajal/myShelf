# My Shelf

A fully offline personal Android media tracker, built with Kotlin and Jetpack Compose, with cover-first browsing and customizable accents.

## Included

- Movies, series, TV shows, comics, books, and other media.
- Collections that span media types, such as Marvel and DC.
- Genre filters scoped to one media type. Select Books before filtering book genres.
- Search across your saved titles, creators, and collections.
- Personal rating (0–5), progress, year, creator, and private notes.
- Manual title entry and covers selected from your device, with persistent image access.
- Local persistence between launches, unsaved-edit confirmation, and delete confirmation.
- A third Settings tab with saved System, Dark, and Light themes and six accent colors.
- Portable ZIP import/export including available covers, with merge or replace on import.
- Privacy controls for a four-digit PIN, optional biometric unlock, and background locking.

No accounts, API keys, catalog calls, analytics, or internet permission. Automatic metadata fetching is excluded: a complete film/book/comic catalog would have to be shipped with the app to offer title lookup offline.

## Run

Open this folder in Android Studio, select a JDK 17+ Gradle runtime, let Gradle sync, and run `app` on Android 8.0 or newer. Install Android SDK 35. Set `sdk.dir` in your untracked `local.properties` if Android Studio does not create it automatically.

```powershell
.\gradlew.bat compileDebugKotlin testDebugUnitTest lintDebug
```

The first development build may download Gradle and Android libraries. The installed app works offline. Do not package APKs unless explicitly requested.

## Backups

In Settings, Export opens Android's save dialog for a My Shelf ZIP. Import opens the file picker, validates the archive, then offers Merge (keep existing entries with matching IDs) or Replace (restore the backup as the entire shelf). Cancel leaves the shelf untouched. The import is validated before any titles are replaced. Appearance settings remain local and are not restored from backups.

Covers are included as bytes rather than device-specific document links. Restored covers are stored in app-private files so the backup works on another device. Unavailable source images are omitted and reported after export. Backups support 2,000 titles, 8 MB per cover, 60 MB total cover data, and 4 MB of metadata. Import rejects malformed or unsupported files, unsafe archive paths, and excessive unpacked data. Save a backup before uninstalling if you want to restore your shelf later.

## App lock

Enable App Lock in Settings, enter four digits, then enter them again to confirm. Disabling the lock requires that PIN. Biometrics can be enabled after PIN setup and a successful Android biometric prompt; unsupported or unenrolled devices keep PIN unlock available.

An enabled lock protects every cold launch. Instant Lock locks when the activity moves into the background; with it off, returning after one minute requires authentication. Rotation preserves the current session, and shelf/editor state is restored after unlocking. File pickers count as leaving the app. Screenshots and recent-app previews are blocked while app lock is enabled.

The app stores a salted HMAC verifier using a device-bound Android Keystore key, not the PIN. Five failed PIN attempts trigger a persisted 30-second cooldown. Lock settings stay on the device and are excluded from exports. App lock is an access gate, not shelf encryption; exported ZIP backups are unencrypted. There is no PIN recovery flow.

## Structure and limits

`ShelfData.kt` owns the model, filtering, and on-device persistence. `ShelfJson.kt` handles metadata serialization and validation; `ShelfBackup.kt` handles portable backups. `MainActivity.kt` contains the shelf and collection views, `EditorScreens.kt` contains editing, and `SettingsScreen.kt` / `ShelfTheme.kt` provide appearance and data controls.

The shelf starts empty. One genre and one collection are supported per title. There is no cloud sync or episode/chapter tracking yet. Library data is stored in app-private preferences, and uninstalling the app removes it. User-picked covers reference the selected document; deleting that document can make the cover unavailable. Missing images use a typographic cover fallback. Choose an image available on your device to use it offline.
