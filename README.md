# My Shelf

A fully offline personal Android media tracker, built with Kotlin and Jetpack Compose. A warm paper palette, forest green accents, and a cover-first shelf keep the focus on your stories.

## Included

- Movies, series, TV shows, comics, books, and other media.
- Collections that span media types, such as Marvel and DC.
- Genre filters scoped to one media type. Select Books before filtering book genres.
- Search across your saved titles, creators, and collections.
- Personal rating (0–5), progress, year, creator, and private notes.
- Manual title entry and covers selected from your device, with persistent image access.
- Local persistence between launches, unsaved-edit confirmation, and delete confirmation.

No accounts, API keys, catalog calls, analytics, or internet permission. Automatic metadata fetching is excluded: a complete film/book/comic catalog would have to be shipped with the app to offer title lookup offline.

## Run

Open this folder in Android Studio, select a JDK 17+ Gradle runtime, let Gradle sync, and run `app` on Android 8.0 or newer. Install Android SDK 35. Set `sdk.dir` in your untracked `local.properties` if Android Studio does not create it automatically.

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

The first development build may download Gradle and Android libraries. The installed app works offline.

## Structure and limits

`ShelfData.kt` owns the model, filtering, and on-device JSON persistence. `MainActivity.kt` contains the shelf and collection views; `EditorScreens.kt` contains editing.

The shelf starts empty. One genre and one collection are supported per title. There is no cloud sync, export, or episode/chapter tracking yet. Library data is stored in app-private preferences, and uninstalling the app removes it. User-picked covers reference the selected document; deleting that document can make the cover unavailable. Missing images use a typographic cover fallback. Choose an image available on your device to use it offline.
