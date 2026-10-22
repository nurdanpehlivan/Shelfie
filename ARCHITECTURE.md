# Shelfie architecture

MVVM with clean layers, split into Gradle modules so each layer can only see what it should.

```
app/                 Activity, navigation between features, widget, notifications, background work
core/domain/         Pure Kotlin: models, repository interfaces, use cases, change rules
core/data/           Room, sync, Shelfie backend, Gemini (via the proxy), Play Billing, OCR, Hilt modules
core/ui/             Theme, shared components, Frizzy, the item editor, all strings and icons
feature/pantry/      The pantry list, filters, edit/freeze/thaw, quick add with barcode
feature/scanner/     Photo scan and review
feature/recipe/      Rescue recipes, cooking mode, ingredient picker, recipe history
feature/mealplan/    Weekly meal plan
feature/shopping/    Shopping list by aisle
feature/statistics/  Rescued vs wasted
feature/settings/    Settings, account, household, Premium
feature/onboarding/  First-run pages
build-logic/         Convention plugins shared by the modules (shelfie.android.library, .feature, ...)
```

## Dependency rules

- `core:domain` depends on nothing Android.
- `core:data` and `core:ui` depend on `core:domain`.
- A feature depends on `core:domain` and `core:ui` only: never on `core:data`, never on another
  feature. Screens talk to repositories through the domain interfaces, which Hilt binds.
- `app` depends on everything and is the only place features meet (`ShelfieNavHost`).

Build facts the UI needs (version, web URLs) reach the features through `LocalAppInfo`, so no
feature reads a `BuildConfig`.

## Inside a feature

Each screen has a `*Contract.kt` (UI state, intents, one-off effects), a `*ViewModel.kt` that
turns intents into state using use cases or repositories, and a `*Screen.kt` with a stateful
`*Route` and a stateless screen composable. Sub-composables live in `components/`.

## Tests

- Unit tests sit next to the code in each module (`src/test`): domain rules and use cases,
  parsers and mappers in data, ViewModels in features.
- `core/data/src/androidTest` covers Room migrations (with the exported schemas in
  `core/data/schemas`) and the sync manager; run them on an emulator.
