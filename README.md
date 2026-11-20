# Flashcards

Study flashcards and test yourself with scored quizzes, on **Android and iOS**.
Built with Kotlin Multiplatform and Compose Multiplatform: one shared codebase for UI, logic, and storage.

## Features

- Four bundled decks (Kotlin basics, world capitals, Spanish basics, element symbols) loaded from a JSON resource
- **Study mode:** cards turn over with a 3D flip animation; swipe or use the arrows to move between cards; shuffle and un-shuffle
- **Quiz mode:** multiple-choice questions built from the deck, with answers revealed in color, a shake for wrong picks, and an animated score counter
- **Scoring:** 100 points per correct answer plus a streak bonus (+10 per answer in a row, up to +50)
- **Results:** the score counts up, a trophy pops in, accuracy and best streak are shown, and a new best score is celebrated
- Best score per deck saved on the device; quiz length is configurable (5, 10 or 15 questions)
- Light / Dark / System theme, plus Material You dynamic color on Android 12+
- Loading, error (with retry), and not-found states; screen-reader labels throughout

## Tech stack

| Concern | Library |
|---|---|
| UI | Compose Multiplatform, Material 3, Compose animations |
| Architecture | MVVM, Unidirectional Data Flow, Repository pattern |
| DI | Koin |
| Content | Compose Multiplatform resources (`files/decks.json`), kotlinx.serialization |
| Persistence | Preferences DataStore (settings and best scores) |
| Navigation | Navigation Compose (multiplatform), type-safe destinations with arguments |
| Testing | kotlin.test, kotlinx-coroutines-test, Turbine (run on Android JVM and iOS simulator) |

## Highlights

| Concept | Where |
|---|---|
| **Bundled JSON → validated domain model** (versioned format, unknown keys ignored, clear errors) | `core/data/deck/DeckParser.kt` |
| **Immutable quiz state machine**: every action returns a new session | `core/domain/QuizSession.kt` |
| **Deterministic randomness**: `Random` is injected, so quizzes are reproducible in tests | `core/domain/QuizGenerator.kt` |
| **3D card flip** with `graphicsLayer` rotation and a counter-rotated back face | `core/designsystem/component/FlipCard.kt` |
| **Animations**: `AnimatedContent` (card and question transitions, rolling score), `animateColorAsState`, `Animatable` (shake, count-up, spring) | `feature/study/`, `feature/quiz/QuizScreen.kt` |
| **Atomic "best score" updates** inside a DataStore transaction | `core/data/repository/ScoreRepository.kt` |

## Adding a deck

Add an entry to `shared/src/commonMain/composeResources/files/decks.json`:

```json
{
  "id": "my-deck",
  "title": "My Deck",
  "description": "Optional short description.",
  "color": "purple",
  "cards": [
    { "front": "Question", "back": "Answer" }
  ]
}
```

`color` is one of `purple`, `teal`, `orange`, `blue`. Each deck needs a unique `id` and at least two cards
with different answers, so a quiz always has a wrong option to offer.

## Architecture

```
Compose screens ──▶ ViewModels ──▶ DeckRepository ──▶ DeckParser ◀── files/decks.json (resource)
                        │      ├─▶ ScoreRepository ───────────▶ DataStore<Preferences>
                        │      └─▶ UserPreferencesRepository ─▶ DataStore<Preferences>
                        └─▶ QuizGenerator, QuizSession, QuizScoring (pure Kotlin)
```

## Project structure

```
shared/                              # Kotlin Multiplatform module: everything shared
├── src/commonMain/kotlin/.../
│   ├── App.kt, AppViewModel.kt      # Root composable; applies the saved theme
│   ├── di/                          # Koin modules + initKoin()
│   ├── navigation/                  # NavHost
│   ├── core/model | domain | data | designsystem
│   └── feature/decks | study | quiz | settings
├── src/commonMain/composeResources/ # decks.json, strings, plurals, vector icons
├── src/androidMain/                 # DataStore path, dynamic color
├── src/iosMain/                     # DataStore path, MainViewController
└── src/commonTest/                  # Parser, domain and ViewModel tests (run on Android + iOS)
androidApp/                          # Android launcher: Application + MainActivity
iosApp/                              # iOS launcher: SwiftUI app hosting the shared UI
```

## Build and run

**Requirements:** JDK 17, Android Studio (latest stable) with Android SDK Platform 37.1.
iOS additionally needs a Mac with Xcode 16+.

### Android

```bash
./gradlew :androidApp:installDebug            # install on a connected device/emulator
./gradlew :shared:testAndroidHostTest         # shared unit tests on the JVM
```

### iOS (macOS only)

```bash
./gradlew :shared:iosSimulatorArm64Test       # shared unit tests on the iOS simulator
open iosApp/iosApp.xcodeproj                  # then press Run in Xcode
```

To run on a physical iPhone, set `TEAM_ID` in `iosApp/Configuration/Config.xcconfig`.

CI builds and tests both platforms on every push (`.github/workflows/ci.yml`).

## Possible next steps

- Spaced repetition (e.g. SM-2) to schedule reviews of cards you get wrong
- Create and edit your own decks in the app
- Import decks from a URL or file
- Typed-answer mode in addition to multiple choice
