# LITS Puzzle Game - Project Intelligence

## Game Rules (LITS)
1. Each region must contain exactly one tetromino (4-cell polyomino) of shape L, I, T, or S.
2. All shaded cells must form a single connected group.
3. No 2×2 area may be entirely shaded.
4. Two regions with the same shape type cannot share an edge.

## UX & Interaction Logic

### Tap Modes (user-configurable)
- **3-tap mode (default):** Single tap cycles `EMPTY → SHADED → MARKED → EMPTY`.
- **2-tap mode:** Single tap toggles `EMPTY ↔ SHADED`. Long press sets `MARKED`. Tap on `MARKED` clears to `EMPTY`.

### Haptics
- Triggered on every tap and long press (not state-dependent).
- Uses `HapticFeedbackConstants.KEYBOARD_TAP` via `LocalView`.
- Can be disabled in Settings. Preference is persisted.

### Color Pattern
All colors are defined as top-level constants in `ui/AppColors.kt`. Never hardcode colors elsewhere.
- **Empty cell:** `ColorCellEmpty` — light grey, same for all regions.
- **Shaded (unvalidated):** `ColorCellShaded` — dark grey.
- **Validated shape:** colored once a region has exactly 4 shaded cells forming a valid polyomino — `ColorShapeL` (blue), `ColorShapeI` (cyan), `ColorShapeT` (purple), `ColorShapeS` (green).
- **Error overlay:** diagonal stripes (`ColorErrorStripe`) on shaded cells in a 2×2 violation or adjacent same-shape conflict.
- **Borders:** `ColorGridLineThick` between regions, `ColorGridLineThin` within a region.

### Strings
All user-facing strings are in `res/values/strings.xml`. Never hardcode UI strings in composables.

## Navigation
```
Welcome → Level Select (per size) → Game
Welcome → Settings
```
- Uses **Jetpack Navigation Compose** with typed int arguments.
- Routes: `"welcome"`, `"levels/{gridSize}"`, `"game/{gridSize}/{levelIndex}"`, `"settings"`.
- Back arrow on Level Select, Game, and Settings screens.
- All screens use `statusBarsPadding()` to avoid the phone's status bar.

## Screens
- **WelcomeScreen:** Grid of size cards (5×5 → 10×10) + top-left settings icon (36dp).
- **LevelSelectScreen:** 3-column grid of level cells. Grey = incomplete, orange = started, green = completed. Completion time shown below number on green cells.
- **GameScreen:** Header (title + back), Hint + Reset buttons (top-left), timer (top-right, hidden in zen mode), grid, status chips, shape legend. Zen mode hides timer, SOLVED banner, status chips, and legend — but not the title.
- **SettingsScreen:** Haptic toggle, Two-tap mode toggle, Zen mode toggle.

## Architecture: Multi-OS Readiness
- **Rule:** `logic/` must stay pure Kotlin (no Android imports). Enables future Compose Multiplatform migration.
- **State:** Single `GameState` data class observed by the UI.
- ViewModels scoped to `AppNavigation` for shared state: `SettingsViewModel`, `ProgressViewModel`.
- `GameViewModel` is an `AndroidViewModel` reading `gridSize` and `levelIndex` from `SavedStateHandle`.
- `LitsApp : Application()` holds the `LevelRepository` singleton, shared across ViewModels.

## Persistent Storage (DataStore)

### `settings` DataStore (`SettingsStore`)
| Key | Type | Default |
|-----|------|---------|
| `haptic_enabled` | Boolean | true |
| `two_tap_mode` | Boolean | false |
| `zen_mode` | Boolean | false |

### `progress` DataStore (`ProgressStore`)
| Key pattern | Type | Content |
|-------------|------|---------|
| `completed_<size>` | StringSet | Completed level indices for that grid size |
| `state_<size>_<index>` | String | In-progress cell states (UInt32 bit-packed, see below) |
| `timer_<size>_<index>` | Long | Elapsed seconds for an in-progress level |
| `ctime_<size>_<index>` | Long | Completion time (seconds) for a solved level |

- `startedLevels` is derived by scanning keys matching `state_<size>_*` — no extra storage needed.
- Level is marked completed in `GameViewModel` on solve: timer stops, completion time is saved, cell state is cleared.
- `GameViewModel.onCleared()` flushes cell states and elapsed time if not solved.

### Cell State Encoding
- 2 bits per cell: `EMPTY=00`, `SHADED=01`, `MARKED=10`.
- Each row packed into a UInt32 (MSB = cell 0). Max size 16 (`check(level.size < 17)`).
- Stored as an 8-char hex string per row, all rows concatenated.

## Level System

### File Format
Levels are plain text files: `assets/levels/<size>/<index>.txt`
- Content: a flat string of lowercase letters, row-by-row left-to-right.
- Encoding: `'a'` = region 0, `'b'` = region 1, …, `'z'` = region 25.
- Grid size is derived from the folder name; string length must equal `size²`.
- Example 5×5: `"aaabbaacbbddccbddccedeeee"`

### Adding Levels
1. Add the level string to the appropriate `LEVELS_<N>` list in `tools/generate_levels.py`.
2. Run `python tools/generate_levels.py --write` to validate and write `.txt` files.
3. No code changes needed — the repository auto-discovers files via `assets.list()`.

### Repository Interface (in `logic/` — pure Kotlin)
```kotlin
interface LevelRepository {
    fun getLevelCount(size: Int): Int
    fun getLevel(size: Int, index: Int): Level?
}
```
- `LevelRepositoryImpl` (in `data/`) implements asset loading and caches results.
- `LevelParser` (in `data/`) converts the string format into a `Level`.
- `LevelSchemaValidator` (in `logic/`) validates that region IDs are contiguous and every region has ≥ 4 cells.

### Level Model
```kotlin
data class Level(val size: Int, val regionGrid: List<List<Int>>, val regionCount: Int)
```
- `regionCells: Map<Int, List<Cell>>` — computed lazily for O(regionSize) lookups during validation.

## Tech Stack
- Framework: Jetpack Compose (100% UI, no XML)
- Language: Kotlin
- Architecture: MVVM
- Navigation: Navigation Compose 2.7.7
- Storage: Jetpack DataStore Preferences 1.1.1
- Icons: material-icons-core (ArrowBack, Settings)
- State: Kotlin Flow + StateFlow

## Project Structure
```
logic/          — Pure Kotlin (no Android): LitsModels, LitsValidator,
                  LevelRepository (interface), LevelSchemaValidator
ui/             — Composables: AppNavigation, WelcomeScreen, LevelSelectScreen,
                  GameScreen (contains GameGrid), SettingsScreen, AppColors
data/           — LevelRepositoryImpl, LevelParser, SettingsStore, ProgressStore
GameViewModel       — AndroidViewModel: game state, cell interactions, timer, persistence
SettingsViewModel   — haptic, two-tap mode, zen mode
ProgressViewModel   — per-size completion tracking
LitsApp             — Application singleton; holds LevelRepository
```

## Naming Conventions
- Composables: PascalCase (e.g., `GameGrid()`)
- Functions: camelCase (e.g., `validateShape()`)
- ViewModels: `{Feature}ViewModel`
- Color constants: `Color{Purpose}` (e.g., `ColorShapeL`, `ColorCellEmpty`)

## Build Commands
- Build: `./gradlew assembleDebug`
- Test: `./gradlew test`
- Lint: `./gradlew ktlintCheck`
- Generate/validate levels: `python tools/generate_levels.py [--write]`
