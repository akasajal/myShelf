# My Shelf

A local-first personal Android media tracker, built with Kotlin and Jetpack Compose, with cover-first browsing and customizable accents.

## Included

- Movies, series, TV shows, comics, books, and other media.
- Collections that span media types, such as Marvel and DC.
- Collection customization: 16 preset icons, editable names, and descriptions, included in portable backups.
- Genre labels on titles, scoped to their media type.
- Search across your saved titles, creators, and collections.
- Personal rating (0–5), progress, year, creator, and private notes.
- Status filtering beside Sort on My Shelf and inside each collection: On my list, In progress, Caught up, Finished, On hold, and Dropped. Each title grid remembers its status choice alongside search and media filters. The collections list shows all collections and their full title counts, with Sort only.
- One card per series or TV show, with individual season status, ratings, and episode counts inside.
- A detail page with immediate status, rating, collection, and episode updates; a separate Edit form handles metadata.
- Long-press title actions, removal with Undo, and duplicate-title warnings when adding or renaming within the same media type.
- Manual title entry and a cover sheet for device images, clipboard images, and direct image links.
- A compact squircle + button for the add form, paired with a left-side Filter button that opens one compact sheet with seven media rows. Sort stays in the top toolbar.
- Local persistence between launches, unsaved-edit confirmation, and delete confirmation.
- A third Settings tab with saved System, Dark, and Light themes and six accent colors.
- A saved Cards per row preference (2, 3, or 4) for the shelf and collection contents.
- Saved Comfortable/Compact card details, plus separate search, media filter, and scroll positions for My Shelf and each collection during navigation and activity restoration.
- Sort menus on My Shelf, the collections list, and each collection's contents. Shelf and individual collections remember their own choices; the collections list supports names, title counts, and recent additions. Title lists support added order, alphabetical order, personal rating, and release year. Unrated titles and unknown years appear last for their respective sorts. Sorting uses the stored addition order and leaves saved entries unchanged.
- Portable ZIP import/export including available covers, with a change preview and skip, update, or replace choices.
- Privacy controls for a four-digit PIN, optional biometric unlock, and background locking.

No accounts, API keys, catalog calls, or analytics. The app works offline; it accesses the internet only when you explicitly add a cover from an HTTPS image link. Automatic metadata fetching is excluded.

## Run

Open this folder in Android Studio, select a JDK 17+ Gradle runtime, let Gradle sync, and run `app` on Android 8.0 or newer. Install Android SDK 35. Set `sdk.dir` in your untracked `local.properties` if Android Studio does not create it automatically.

```powershell
.\gradlew.bat compileDebugKotlin testDebugUnitTest lintDebug
```

The first development build may download Gradle and Android libraries. The installed app works offline. Do not package APKs unless explicitly requested.

## Backups

In Settings, Export opens Android's save dialog for a My Shelf ZIP. Import validates the archive and previews new, matching, and removed titles. Skip existing adds new IDs and keeps matching entries. Update matching adds new IDs and replaces all metadata and covers for matching IDs, while preserving unrelated saved titles. Replace shelf restores the backup as the entire shelf and requires a second confirmation. Matching uses stable IDs, so different titles with the same name remain distinct. Cancel leaves the shelf untouched. Appearance settings remain local and are not restored from backups.

Covers are included as bytes rather than device-specific document links. Restored covers are stored in app-private files so the backup works on another device. Unavailable source images are omitted and reported after export. Backups support 2,000 titles, 8 MB per cover, 60 MB total cover data, and 4 MB of metadata. Import rejects malformed or unsupported files, unsafe archive paths, and excessive unpacked data. Save a backup before uninstalling if you want to restore your shelf later.

## Cover images

Tap the cover or doodle to open a bottom sheet: Choose image, Paste image, Paste image link, and Remove cover when present. Paste image expects an actual clipboard image URI; copied website links belong in Paste image link. The link field can be edited before tapping Add cover and expects a direct HTTPS image, not a web page. Clipboard contents are read only after a paste action.

All new covers are validated, resized to at most 1,600 pixels on the longest edge, and saved as app-private JPEGs. Downloads are limited to 8 MB with timeouts and HTTPS-only redirects. An unsuccessful or cancelled import preserves the current cover. Once saved, gallery, pasted, and downloaded covers work offline and are included in backups. Existing cover document links from older versions remain supported.

## Collections

Tap + on the Collections list to create a collection with a name, description, and preset icon. Inside a collection, + adds a title to it. Tap the pencil on a collection card or inside a collection to customize it. Choose from 16 local Material icons. Renaming updates every title in that collection and keeps its identity, saved browsing context, and sort preference. Names must be unique ignoring case and surrounding spaces. Customizations remain saved even when the collection has no titles.

Collection names, descriptions, and icon choices travel with ZIP backups. Skip existing keeps matching collection customizations; Update matching applies incoming customizations and follows collection IDs through renames; Replace shelf uses only the backup's collections. Conflicting renames are rejected without applying the import. Older backups without collection details remain supported.

## Seasons

Open a series or TV show and tap Add season. Each season has a unique number, optional name, status, rating, watched episode count, and optional total. The detail page provides episode −/+ buttons, Mark season finished, and a season edit icon. Done saves season changes immediately on the detail page. In the full title editor, season changes remain part of the draft until Save. Marking a season Finished fills its known episode total. Adding seasons to existing titles is optional and leaves other titles unchanged.

With seasons, the card shows a completion count and derives its status from them. Completing all tracked seasons shows Caught up; enable Series has ended to show Finished instead. Add newly released seasons manually. Season details stay on-device and travel with ZIP backups; older backups without seasons still import.

Dropped is available for titles and individual seasons. Use Drop series in the detail page's status menu to stop tracking a show without changing completed seasons; Resume series clears that choice. Updating episode progress also resumes it. The full editor provides Drop/Resume beside the derived status.

## App lock

Enable App Lock in Settings, enter four digits, then enter them again to confirm. Disabling the lock requires that PIN. Biometrics can be enabled after PIN setup and a successful Android biometric prompt; unsupported or unenrolled devices keep PIN unlock available.

Setup, confirmation, disabling, and unlocking share a full-screen numeric keypad with four masked dots and backspace. Entering the fourth digit advances or submits automatically. No system keyboard or extra submit button is needed. The setup screen has a back arrow; unlock offers biometrics when enabled and available.

An enabled lock protects every cold launch and always locks immediately when the activity moves into the background. There is no delay or separate Instant Lock setting; previous delay preferences are ignored. Rotation preserves the current session, and shelf/editor state is restored after unlocking. File pickers count as leaving the app. Screenshots and recent-app previews are blocked while app lock is enabled.

The app stores a salted HMAC verifier using a device-bound Android Keystore key, not the PIN. Five failed PIN attempts trigger a persisted 30-second cooldown. Lock settings stay on the device and are excluded from exports. App lock is an access gate, not shelf encryption; exported ZIP backups are unencrypted. There is no PIN recovery flow.

## Structure and limits

`ShelfData.kt` owns the model, filtering, and on-device persistence. `ShelfJson.kt` handles metadata serialization and validation; `ShelfBackup.kt` handles portable backups. `MainActivity.kt` contains the shelf and collection views, `EditorScreens.kt` contains editing, and `SettingsScreen.kt` / `ShelfTheme.kt` provide appearance and data controls.

`TitleDetails.kt` and `TitleActions.kt` provide details and quick actions. `ShelfChanges.kt` owns import decisions, duplicate matching, and deletion restoration. `BrowsingContext.kt` saves per-screen searches and filters; scroll positions use Compose's saveable list/grid states.

The shelf starts empty. One genre and one collection are supported per title. There is no cloud sync, automatic season catalog, individual episode metadata, or chapter tracking. Library data and new covers are stored privately on the device, and uninstalling the app removes them. Older cover document links may become unavailable if their source document is deleted. Missing covers use a doodle or typographic fallback.
