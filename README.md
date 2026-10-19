# My Shelf

A local-first personal Android media tracker, built with Kotlin and Jetpack Compose, with cover-first browsing and customizable accents.

## Included

- Movies, series, TV shows, comics, books, and other media.
- Collections that span media types, such as Marvel and DC.
- Genre filters scoped to one media type. Select Books before filtering book genres.
- Search across your saved titles, creators, and collections.
- Personal rating (0–5), progress, year, creator, and private notes.
- Manual title entry and a cover sheet for device images, clipboard images, and direct image links.
- Local persistence between launches, unsaved-edit confirmation, and delete confirmation.
- A third Settings tab with saved System, Dark, and Light themes and six accent colors.
- Portable ZIP import/export including available covers, with merge or replace on import.
- Privacy controls for a four-digit PIN, optional biometric unlock, and background locking.

No accounts, API keys, catalog calls, or analytics. The app works offline; it accesses the internet only when you explicitly add a cover from an HTTPS image link. Automatic metadata fetching is excluded.

## Run

Open this folder in Android Studio, select a JDK 17+ Gradle runtime, let Gradle sync, and run `app` on Android 8.0 or newer. Install Android SDK 35. Set `sdk.dir` in your untracked `local.properties` if Android Studio does not create it automatically.

```powershell
.\gradlew.bat compileDebugKotlin testDebugUnitTest lintDebug
```

The first development build may download Gradle and Android libraries. The installed app works offline. Do not package APKs unless explicitly requested.

## Backups

In Settings, Export opens Android's save dialog for a My Shelf ZIP. Import opens the file picker, validates the archive, then offers Merge (keep existing entries with matching IDs) or Replace (restore the backup as the entire shelf). Cancel leaves the shelf untouched. The import is validated before any titles are replaced. Appearance settings remain local and are not restored from backups.

Covers are included as bytes rather than device-specific document links. Restored covers are stored in app-private files so the backup works on another device. Unavailable source images are omitted and reported after export. Backups support 2,000 titles, 8 MB per cover, 60 MB total cover data, and 4 MB of metadata. Import rejects malformed or unsupported files, unsafe archive paths, and excessive unpacked data. Save a backup before uninstalling if you want to restore your shelf later.

## Cover images

Tap the cover or doodle to open a bottom sheet: Choose image, Paste image, Paste image link, and Remove cover when present. Paste image expects an actual clipboard image URI; copied website links belong in Paste image link. The link field can be edited before tapping Add cover and expects a direct HTTPS image, not a web page. Clipboard contents are read only after a paste action.

All new covers are validated, resized to at most 1,600 pixels on the longest edge, and saved as app-private JPEGs. Downloads are limited to 8 MB with timeouts and HTTPS-only redirects. An unsuccessful or cancelled import preserves the current cover. Once saved, gallery, pasted, and downloaded covers work offline and are included in backups. Existing cover document links from older versions remain supported.

## App lock

Enable App Lock in Settings, enter four digits, then enter them again to confirm. Disabling the lock requires that PIN. Biometrics can be enabled after PIN setup and a successful Android biometric prompt; unsupported or unenrolled devices keep PIN unlock available.

Setup, confirmation, disabling, and unlocking share a full-screen numeric keypad with four masked dots and backspace. Entering the fourth digit advances or submits automatically. No system keyboard or extra submit button is needed. The setup screen has a back arrow; unlock offers biometrics when enabled and available.

An enabled lock protects every cold launch. Instant Lock locks when the activity moves into the background; with it off, returning after one minute requires authentication. Rotation preserves the current session, and shelf/editor state is restored after unlocking. File pickers count as leaving the app. Screenshots and recent-app previews are blocked while app lock is enabled.

The app stores a salted HMAC verifier using a device-bound Android Keystore key, not the PIN. Five failed PIN attempts trigger a persisted 30-second cooldown. Lock settings stay on the device and are excluded from exports. App lock is an access gate, not shelf encryption; exported ZIP backups are unencrypted. There is no PIN recovery flow.

## Structure and limits

`ShelfData.kt` owns the model, filtering, and on-device persistence. `ShelfJson.kt` handles metadata serialization and validation; `ShelfBackup.kt` handles portable backups. `MainActivity.kt` contains the shelf and collection views, `EditorScreens.kt` contains editing, and `SettingsScreen.kt` / `ShelfTheme.kt` provide appearance and data controls.

The shelf starts empty. One genre and one collection are supported per title. There is no cloud sync or episode/chapter tracking yet. Library data and new covers are stored privately on the device, and uninstalling the app removes them. Older cover document links may become unavailable if their source document is deleted. Missing covers use a doodle or typographic fallback.
