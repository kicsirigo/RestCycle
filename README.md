<div align="center">

# RestCycle

**Native Android Sleep Cycle, Ambient Sound & Sleep Journal App**  
*Zero Tracking • 120Hz AMOLED Optimized • Dynamic Celestial Atmosphere Engine*

[![Android](https://img.shields.io/badge/Android-8.0%2B%20(API%2026%E2%80%9336)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Tests](https://img.shields.io/badge/Tests-342%20Passing-2EA043?style=for-the-badge&logo=checkmarx&logoColor=white)](#testing)
[![Privacy](https://img.shields.io/badge/Privacy-Zero%20Tracking-0F172A?style=for-the-badge&logo=shield&logoColor=white)](#privacy--permissions)

</div>

---

## Overview

**RestCycle** is a minimalist, zero-latency sleep cycle calculator, ambient sleep soundscape player, and all-time sleep journal built natively for Android with **Kotlin**, **Jetpack Compose**, and **Material 3**. Designed for effortless one-handed use in the thumb zone, RestCycle calculates optimal sleep and wake windows based on **90-minute REM sleep cycles** and your personal **sleep latency** — with zero ads, zero telemetry, unified monochrome vector iconography, and an optional in-app GitHub Releases updater.

Every interaction reacts in real time: as you scroll the haptic time wheel or switch modes, the entire sky, UI palette, and seamless alpine landscape smoothly transition across **Day**, **Dawn / Dusk**, and **Night** atmospheres.

---

## Screenshots

<div align="center">

| Wake Up At *(Night)* | Sleep Now *(Day)* | Power Nap *(Nord · Twilight)* | Go to Bed At *(Night)* |
| :---: | :---: | :---: | :---: |
| <img src="docs/screenshots/01_wake_up_night.png" alt="Wake Up At - Night" width="210" /> | <img src="docs/screenshots/02_sleep_now_day.png" alt="Sleep Now - Day" width="210" /> | <img src="docs/screenshots/03_power_nap_nord.png" alt="Power Nap - Nord Twilight" width="210" /> | <img src="docs/screenshots/04_catppuccin_night.png" alt="Go to Bed At - Night" width="210" /> |

</div>

---

## Key Features

### 4 Reactive Calculation Modes
| Mode | Purpose | How It Works |
| :--- | :--- | :--- |
| **Wake Up At** | Plan your bedtime | Pick when you want to wake up; RestCycle works backwards in 90-minute cycles minus your sleep latency. |
| **Sleep Now** | Instant wake targets | Uses your live device clock + sleep latency to calculate optimal wake-up times for 6 down to 1 cycles. |
| **Go to Bed At** | Plan future sleep | Choose a planned bedtime and see every optimal wake-up window ahead. |
| **Power Nap** | Science-backed naps | One-tap **10 min** (Micro), **20 min** (Optimal), and **90 min** (Full Cycle) presets with built-in sleep-inertia guidance. |

### Ambient Sleep Sounds, Sleep Timer & Screen-Off Playback
- **6 High-Fidelity Built-In Offline Soundscapes** — **Ocean Waves**, **Thunderstorm**, **Fireplace**, **Night Crickets**, **432 Hz Ambient**, and **Singing Bowl** bundled directly in the APK (zero internet required).
- **Universal Sound Picker Dialog** — Accessible from the top status bar across all 4 modes as well as inside the landscape scene and breathing exercise, with a live **Volume slider (`10%–100%`)** and **Sleep Timer** (`∞`, `15m`, `30m`, `45m`, `60m`) featuring a smooth **30-second fade-out** before stopping.
- **Continuous Screen-Off Audio** — Powered by a persistent `mediaPlayback` foreground service (`AmbientSoundService`) with notification playback controls so audio keeps playing reliably when you lock your phone or switch apps.

### All-Time Sleep Calendar, Per-Minute Manual Log & Personal Insights
- **All-Time Monthly Sleep Calendar** — Browse every month of your sleep history with all-time KPI cards (**Nights**, **Avg sleep**, **Streak**), per-day mood indicators, daily session details, and single-tap entry removal.
- **Quick-Tap Protection** — Accurately measures actual elapsed sleep duration without fabricating cycles on accidental or short sessions.
- **Per-Minute Manual Retroactive Entry (`+ Log`)** — Log forgotten past nights on any day `<= today` using the embedded **HapticTimeWheel** (exact hour & minute scrolling) plus `±1 min` fine-tune steppers, live duration/cycle preview, and 4 wake-up mood ratings (**Exhausted**, **Fair**, **Rested**, **Energized**).
- **Personal Sleep Cycle Pattern Insight** — Once you log at least 2 multi-hour nights, RestCycle analyzes which cycle count leaves you feeling rested or energized most consistently.
- **Offline JSON Backup (Export / Import)** — Export your entire All-Time Sleep Calendar to a portable `.json` file and import/merge backups via Android's Storage Access Framework in Settings without duplicating existing nights.

### Dynamic Celestial Atmosphere Engine & 6 Curated Themes
All **6 developer-grade themes** dynamically adapt across **3 time-of-day phases (`DAY`, `DAWN_DUSK`, `NIGHT`)** — synchronized across the sky backdrop, cards, monochrome vector icons, status bar, and landscape illustration, plus an optional **Pure AMOLED Black (`#000000`)** night mode toggle:

- **Tokyo Night** *(Default)* — Crisp Tokyo Day, warm Neon Twilight, and deep Tokyo Night.
- **Nord** — Polar Snow Storm by day, Frost & Aurora Twilight at dawn/dusk, and Polar Night at night.
- **Everforest** — Warm botanical parchment by day, mossy twilight, and deep forest night.
- **Gruvbox** — Warm retro parchment daylight, Ember Twilight, and Gruvbox Dark night.
- **Dracula** — Bright daylight lavender, violet twilight, and classic Dracula night.
- **Rosé Pine** — Soft Dawn daylight, dusky rose twilight, and Rosé Pine Moon night.

### One-Handed Ergonomics, Iconography & Utilities
- **Unified Monochrome Vector Iconography & Minimalist Moon Icon** — Zero Unicode emojis anywhere in the UI; every button, pill, setting, mood, and sound uses crisp 24dp monochrome vector paths dynamically tinted to the active theme, paired with a minimalist crescent moon adaptive launcher icon.
- **3D Cylindrical Haptic Time Wheel & Quick Time Chips** — GPU-accelerated 3D cylinder perspective with edge fade, tactile `CLOCK_TICK` feedback, tabular numerals (`tnum`), automatic `AM ↔ PM` toggling across `11 ↔ 12`, and one-tap preset chips (`06:00–07:30` / `22:00–00:00`).
- **Borderless Alpine Landscape Scene** — Custom Jetpack Compose `Canvas` alpine landscape in *Sleep Now* and *Power Nap* modes with living time-of-day micro-animations.
- **4-7-8 Sleep Breathing Guide** — Built-in 120Hz breathing orb animation (`4s Inhale • 7s Hold • 8s Exhale`) with subtle haptic cues and quick ambient sound access.
- **Caffeine Cutoff Indicator & Bedtime Reminder** — Displays your recommended last caffeine time (Off / 6h / 8h / 10h before bedtime) plus an optional local `AlarmManager` wind-down notification 20 minutes before bedtime.
- **Home Screen Widget & App Shortcuts** — Glanceable 4×1 home screen widget displaying live 5-cycle & 6-cycle wake windows, plus long-press launcher shortcuts (*Sleep Now*, *Power Nap*, *Breathe*).
- **Native Alarm & Timer Integration** — Tap any cycle card to launch Android's native `AlarmClock` (`ACTION_SET_ALARM` / `ACTION_SET_TIMER`) with the exact target time and label.
- **7-Language Localization & In-App Switcher** — Switch languages anytime inside Settings (**Auto**, **English**, **Magyar**, **Español**, **Română**, **Polski**, **Português**, **Português (BR)**) with strict lint translation parity checks.
- **In-App Update Download Manager & Direct Installer** — Opening Settings quietly checks GitHub Releases against your installed version; when an update is available, you can download the signed APK directly in-app with a live progress bar, MB readout, Cancel button, and one-tap system installer handoff via `FileProvider` (**never forced**).

---

## Privacy & Permissions

RestCycle has **zero ads, zero analytics, and zero background tracking**:
- `com.android.alarm.permission.SET_ALARM` — Sets alarms and timers in your system Clock app when you tap a sleep option.
- `android.permission.INTERNET` — Used strictly when you check for or download an update from `github.com/kicsirigo/RestCycle`.
- `android.permission.POST_NOTIFICATIONS` — Used for optional bedtime wind-down reminders and the ambient sound playback notification.
- `android.permission.FOREGROUND_SERVICE` & `android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK` — Keeps ambient sleep audio playing reliably when the screen turns off.
- `android.permission.REQUEST_INSTALL_PACKAGES` — Enables launching Android's system package installer directly after downloading an update APK inside Settings.
- **Local Storage Only:** All preferences and your All-Time Sleep Calendar history are stored locally on-device via **Jetpack DataStore** (and portable offline `.json` backups whenever you choose to export).

---

## Download & Verification

Pre-built APKs are available on the [**Releases**](https://github.com/kicsirigo/RestCycle/releases) page:

- **`RestCycle-release.apk`** (~9.0 MB, includes 6 bundled hi-fi MP3 ambient soundscapes) — Recommended. R8-minified, resource-shrunk, and signed with the project release key.
- **`RestCycle-debug.apk`** (~20 MB) — Unminified debug build for testing and inspection.

### Verify APK Integrity
Download `SHA256SUMS.txt` from the release and verify locally:
```bash
sha256sum -c SHA256SUMS.txt
```

### Verify Signing Certificate (Release APK)
```bash
apksigner verify --print-certs RestCycle-release.apk
```
Expected signing certificate:
- **Subject DN:** `CN=kicsirigo, OU=Mobile, O=RestCycle, L=Budapest, C=HU`
- **SHA-256:** `479743c1db8934a55692cdc66bd77e3b6b8312c0b68351ccd13be5ead85075b1`
- **SHA-1:** `5bfc1e8b14cac421884a244f0895dea531d74dfd`

> **Note on Google Play Protect:** Because RestCycle is distributed directly outside the Google Play Store, signed with an independent developer key, and includes an optional in-app GitHub Release updater (`REQUEST_INSTALL_PACKAGES`), Google Play Protect may display a warning on installation. You can verify the SHA-256 checksum and signing certificate above or build the APK directly from this source code, then choose **More details → Install anyway**.

---

## Tech Stack & Architecture

- **Language:** Kotlin 2.1 (JVM 21 toolchain)
- **UI:** Jetpack Compose + Material 3 (Edge-to-Edge, `Canvas` rendering, `CompositionLocal` atmosphere provider)
- **State & Persistence:** `ViewModel`, `StateFlow`, `androidx.datastore:datastore-preferences`
- **Min / Target SDK:** API 26 (Android 8.0) / API 36 (Android 16)
- **Testing:** JUnit 4, Kotlin Coroutines Test, Robolectric, Compose UI Test, Espresso Intents

```text
app/src/main/java/com/restcycle/app/
├── MainActivity.kt                          # Edge-to-edge host, SAF backup launchers, shortcut intents
├── RestCycleApplication.kt                  # Application entry point & DI container
├── data/preferences/
│   └── LatencyPreferencesRepository.kt      # DataStore persistence (latency, theme, history, settings)
├── domain/
│   ├── calculator/
│   │   ├── SkyPhaseResolver.kt              # Time-of-day atmosphere phase resolver
│   │   └── SleepCycleCalculator.kt          # 90-min cycle math, Power Nap presets, midnight rollover
│   └── model/
│       ├── SleepContracts.kt                # Modes, TimeFormatPreference, AtmospherePhase, domain models
│       └── SleepHistoryRecord.kt            # All-time history codec, cycle insight analytics & JSON backup
├── platform/
│   ├── audio/
│   │   ├── AmbientSoundGenerator.kt         # 6 standalone MP3 soundscapes, fade math & playback state
│   │   └── AmbientSoundService.kt           # mediaPlayback ForegroundService & sleep timer countdown
│   ├── intent/AlarmScheduler.kt             # Native AlarmClock alarm & timer handoff
│   ├── reminder/                            # Local AlarmManager bedtime wind-down notifications
│   ├── time/TimeFormatProvider.kt           # System / 12h / 24h clock format provider
│   ├── update/GitHubUpdateChecker.kt        # GitHub Releases check, streaming downloader & FileProvider installer
│   └── widget/RestCycleWidgetProvider.kt    # 4×1 home screen AppWidget
└── ui/
    ├── screen/RestCycleScreen.kt            # Adaptive thumb-zone layout & mode orchestration
    ├── viewmodel/SleepViewModel.kt          # Reactive unidirectional state flow
    ├── components/
    │   ├── AmbientSoundDialog.kt            # Sound picker, volume slider & sleep timer chips
    │   ├── BreathingExerciseDialog.kt       # 4-7-8 breathing orb with haptic pacing
    │   ├── CelestialHeader.kt               # Header with live status & Settings button
    │   ├── CelestialLandscapeScene.kt       # Borderless alpine Canvas scene
    │   ├── HapticTimeWheel.kt               # 12h/24h snapping wheel with CLOCK_TICK & AM/PM auto-flip
    │   ├── LatencySlider.kt                 # Animated 0–60 min sleep latency slider
    │   ├── ModeSegmentedBar.kt              # Spring-animated 4-mode navigation pill
    │   ├── PowerNapSection.kt               # 10 / 20 / 90 min power nap selector + inertia tip
    │   ├── RestCycleIcons.kt                # Unified 24dp monochrome vector icon library
    │   ├── SettingsDialog.kt                # Themes, clock format, languages, backup & in-app updater
    │   ├── SleepOptionCard.kt               # Cycle cards with quality badges & alarm handoff
    │   └── SleepTrackerDialog.kt            # All-time calendar, per-minute manual log & cycle insight
    └── theme/
        ├── AtmosphereTheme.kt               # Unified 3-phase color interpolation across 6 themes + AMOLED
        └── Type.kt                          # Typography with tabular numerals (tnum)
```

---

## Building from Source

### Prerequisites
- **JDK 21**
- **Android SDK** with Build-Tools & Platform API 36

### Build Commands
```bash
# Clone the repository
git clone https://github.com/kicsirigo/RestCycle.git
cd RestCycle

# Run all 342 unit & Compose/Robolectric E2E tests
./gradlew testDebugUnitTest

# Run Android Lint checks
./gradlew lintDebug

# Build Debug APK
./gradlew assembleDebug

# Build Release APK (uses debug signing automatically if no release keystore is configured)
./gradlew assembleRelease
```

To sign a release build with your own keystore, supply the following environment variables or Gradle properties:
```bash
export RESTCYCLE_STORE_FILE="path/to/your-keystore.jks"
export RESTCYCLE_STORE_PASSWORD="your-store-password"
export RESTCYCLE_KEY_ALIAS="your-key-alias"
export RESTCYCLE_KEY_PASSWORD="your-key-password"
./gradlew assembleRelease
```

---

## Testing

The test suite includes **342 automated tests** across 25 test suites covering:
- Sleep cycle math, midnight/DST boundaries, quick-tap protection, and Power Nap calculations
- Dynamic Celestial Atmosphere Engine phase detection & color interpolation across all 6 themes + Pure AMOLED Black
- All-Time Sleep Calendar, per-minute manual logging, personal cycle pattern insight, and offline JSON export/import/merge
- Ambient audio track resources, fade-out math, and 2.5-hour stereo curve bounds
- In-app GitHub Release update checker, redirect-following streaming downloader, and `FileProvider` install intent generation
- 7-language localization parity and monochrome vector icon integrity
- End-to-end Compose UI & Robolectric Tier 1–4 verification (12h/24h wheel rollover, mode switching, settings dialog, alarm intent dispatch)

---

## Credits

**made by: kicsirigo**
