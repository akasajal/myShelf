# Source architecture

The app uses a feature-oriented UI with separate models, rules, and data handling. Kotlin package names match their directories beneath `app/src/main/java/com/myshelf/app/`.

```text
com/myshelf/app/
├── MainActivity.kt          Android lifecycle, biometric prompt, Compose host
├── core/model/             Shared framework-free models and preference enums
├── domain/
│   ├── backup/             Pure import preview and merging rules
│   ├── collections/        Collection creation, validation, renaming, merging
│   ├── home/               Home preview selection
│   ├── library/            Filtering, sorting, duplicates, removal restoration
│   ├── seasons/            Season progress and derived series status
│   └── security/           PIN verification and lock-session rules
├── data/
│   ├── backup/             Portable ZIP reading and writing
│   ├── cover/              Bounded image input, decoding, and private storage
│   └── local/              Shelf and collection JSON codecs
├── features/
│   ├── collections/        Collection list and customization sheet
│   ├── cover/              Cover picker sheet and its ViewModel
│   ├── details/            Title details and quick actions
│   ├── editor/             Title editing and duplicate-warning flow
│   ├── home/               Fixed Home layout, carousels, welcome doodle
│   ├── seasons/            Season editing controls
│   ├── security/           PIN UI and app-lock ViewModel
│   ├── settings/           Settings and import preview UI
│   └── shelf/              Title grid, screen state, and card components
├── navigation/             Destinations, routing, saved browsing context
├── state/                  Shared shelf ViewModel and persisted preferences
└── ui/
    ├── components/         Reusable covers, icons, buttons, menus, empty states
    └── theme/              Compose theme and accent color mapping
```

## Dependency direction

- `core/model` depends on Kotlin/JDK types only. Models do not own UI, JSON, or persistence.
- `domain` depends on shared models and other domain rules. It has no Android, Compose, JSON, or data-layer dependencies.
- `data` handles I/O and serialization using models and domain validation.
- `state` and feature ViewModels coordinate state, data operations, and domain rules.
- Feature screens render their inputs and expose callbacks. Shared `ui` components contain reusable presentation and theme code.
- `navigation` connects destinations, callbacks, and state holders. `MainActivity` hosts the app and integrates the Android lifecycle and biometric prompt.

The shelf screen is reused for My Shelf and collection contents. The collection list has its own screen. Image decoding and storage live under `data/cover`; the cover picker ViewModel handles job cancellation, progress, and UI results.

## Persistence and compatibility

This reorganization preserves the application ID, Activity manifest entry, SharedPreferences names and keys, backup version, and serialized field names. Models and rules moved to new packages; existing user data requires no migration.

Unit tests mirror the corresponding `domain` and `data` package paths under `app/src/test/java/com/myshelf/app/`. Run focused checks without creating an APK:

```powershell
.\gradlew.bat compileDebugKotlin testDebugUnitTest lintDebug --offline
```
