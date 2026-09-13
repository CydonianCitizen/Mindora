# Mindora

Mindora is an open-source, offline-first Android mindfulness application built for calm, private practice. It provides standalone free meditation timers, guided breathing exercises, a library of fifteen text-guided meditations, a looping White Noise timer, ordered mindfulness paths with bundled audio support, gentle haptic feedback, local practice session history, weekly goals, local reminders, and a modern Material 3 design system built with Jetpack Compose.

---

## Current Status

Core MVP engineering is implemented. The production catalogue ships a library of fifteen meditations, in English and Italian, across five techniques: breath awareness, body scan, loving-kindness, open monitoring and yoga nidra. Library practices are text-guided: they run on the meditation timer with their instructions shown one at a time, and no recorded audio is bundled for them yet. Mindfulness paths remain empty in production builds, so no paths or guided audio meditations are visible. Standalone free meditation, breathing and White Noise sessions are fully accessible. One ambient sound is bundled for White Noise; see the attribution section below.

Mindora operates entirely offline. It has no user accounts, cloud backend, analytics, telemetry, advertising, remote configuration, or network communications. Session history is stored locally in Room; small preferences and settings are stored locally in DataStore. Library and path content is read from a bundled JSON catalogue. Guided audio, when approved, is bundled directly with the application.

---

## Privacy Policy

Mindora is designed to operate strictly offline and respects user privacy by default.

### Data Collection & Storage
- **No Remote Telemetry**: The app does not create accounts, collect analytics, send crash reports, or transmit session and preference data to any server.
- **Local Persistence**: Finalized practice sessions are stored locally on the device using Room. The weekly goal, whether the daily reminder is enabled, the reminder time, and the vibration intensity are stored locally in Preferences DataStore. The app language is kept by Android's per-app language setting, not by Mindora. There is no theme preference and no default-timer preference: the app follows the system light/dark setting, and the timer duration is chosen per session.
- **No Cloud Sync or Backups**: Android cloud backup (`allowBackup = false`) and device-to-device data transfer exclusions are explicitly declared. Clearing app data or uninstalling Mindora permanently removes all local data.

### Permissions
- **Notifications**: Notification permission is requested only after the user explicitly enables local reminders. Android system alarm and notification managers handle local reminder delivery, and the reminder is rescheduled after a restart, an app update or a time or time-zone change.
- **Foreground Services & Audio**: Foreground service and media playback permissions support continuous background audio playback for guided sessions and White Noise. Notification permission is not required for playback.
- **Vibration**: Used only for the optional haptic feedback during breathing exercises and free meditation. Its intensity can be lowered or turned off in Settings.

### Scope & Policy URL
This privacy description covers Mindora application behavior. It does not govern third-party platform behavior (such as Android OS, device manufacturers, or app stores). Currently, no public privacy-policy URL is hosted; distribution channels requiring a hosted policy URL remain blocked until one is published.

---

## Third-Party Assets & Attribution

No third-party meditation recordings, background music, or external images are included in production builds. Two open-source typefaces and one ambient sound for White Noise are bundled, and are documented below together with the background of the library meditation texts.

### Bundled Fonts

| File | Family & version | Author & source | License | Modifications |
|---|---|---|---|---|
| `app/src/main/res/font/fraunces.ttf` | Fraunces, variable, version 1.000 | The Fraunces Project Authors, <https://github.com/undercasetype/Fraunces> | SIL Open Font License 1.1, full text at `app/src/main/assets/licenses/Fraunces-OFL.txt` | None. Bundled as released upstream: variable axes, name records and glyph set are intact, with no subsetting or instancing. |
| `app/src/main/res/font/karla.ttf` | Karla, variable, version 2.004 | The Karla Project Authors, <https://github.com/googlefonts/karla> | SIL Open Font License 1.1, full text at `app/src/main/assets/licenses/Karla-OFL.txt` | None. Bundled as released upstream (gftools build), with no subsetting or instancing. |

The OFL requires that the license text travel with the fonts and that reserved font names are not applied to modified versions. Both license files ship inside the APK under `assets/licenses/`, and neither font is modified or renamed.

### Bundled Audio

| File | Description | Author & source | License | Modifications |
|---|---|---|---|---|
| `app/src/main/assets/audio/white_noise.mp3` | "Soft Soothing Deep White Noise": MP3, 44.1 kHz, 256 kbps, 5 minutes; looped by the app for the length of a White Noise session | TheMediaGuy, via Pixabay, <https://pixabay.com/sound-effects/film-special-effects-soft-soothing-deep-white-noise-378857/> | Pixabay Content License, <https://pixabay.com/service/license-summary/>. No attribution required. | None. Byte-identical to the Pixabay download (SHA-256 `20adcf7d57614dee15146f1d8370f72c8ae12916b98375a73b56eb19e135d562`), renamed to `white_noise.mp3`. |

The Pixabay Content License allows the sound to be used inside a larger work such as this app, but not to be distributed on a standalone basis. It is included only as part of Mindora: to reuse the sound on its own, download it from Pixabay under Pixabay's terms.

### Library Content

The fifteen library meditations in `app/src/main/assets/content/mindfulness_catalog.json`, in English and Italian, are Mindora's own wording of widely taught techniques. They were adapted from publicly available descriptions of these practices whose individual sources were not recorded, and no passage is known to reproduce a published script. The references below document each technique; they are background reading, not the recorded source of the text.

- **Breath awareness**: Greater Good Science Center, UC Berkeley, "Mindful Breathing", <https://ggia.berkeley.edu/practice/mindful_breathing>
- **Body scan**: Greater Good Science Center, UC Berkeley, "Body Scan Meditation", <https://ggia.berkeley.edu/practice/body_scan_meditation>; U.S. Department of Veterans Affairs, Whole Health Library, "A Body Scan Script", <https://www.va.gov/WHOLEHEALTHLIBRARY/docs/Script-Body-Scan.pdf>
- **Loving-kindness**: Greater Good Science Center, UC Berkeley, "Loving-Kindness Meditation", <https://ggia.berkeley.edu/practice/loving_kindness_meditation>; Hofmann, S. G., Grossman, P., & Hinton, D. E. (2011). Loving-kindness and compassion meditation: Potential for psychological interventions. *Clinical Psychology Review*, 31(7), 1126–1132. <https://doi.org/10.1016/j.cpr.2011.07.003>
- **Open monitoring**: Lutz, A., Slagter, H. A., Dunne, J. D., & Davidson, R. J. (2008). Attention regulation and monitoring in meditation. *Trends in Cognitive Sciences*, 12(4), 163–169. <https://doi.org/10.1016/j.tics.2008.01.005>
- **Yoga nidra**: Ghai et al. (2026). Effects of Yoga Nidra on Stress, Anxiety, and Depression: A Systematic Review and Meta-Analysis. *Annals of the New York Academy of Sciences*. <https://doi.org/10.1111/nyas.70149>

### Asset Policy & Guidelines
`mindora_logo.svg` at the repository root is first-party original artwork created for Mindora. Android launcher icon resources are generated from this source file.

Any future third-party creative media added to the repository must comply with the following:
- Explicit license permitting redistribution within an open-source application.
- Complete documentation recorded in `README.md` containing:
  - Filename and repository path
  - Author and source URL
  - Exact license terms and attribution requirements
  - Summary of modifications made
- Vague labels such as "Royalty free" or "Copyright free" are not sufficient. Public-domain musical compositions do not automatically make specific audio recordings public domain.

---

## Branding

- Source Logo: `mindora_logo.svg` (located at repository root)
- Launcher Assets: Generated from `mindora_logo.svg` for Android standard and adaptive launcher icons.

---

## Architecture & Project Structure

Mindora follows modern Android development practices with a pragmatic single-module architecture organized by feature and core domain layers:

### Technology Stack
- **Language**: Kotlin 2.3.10
- **UI Framework**: Jetpack Compose, Material 3, Navigation Compose
- **Typography**: Fraunces and Karla, bundled as app font resources (see attribution above)
- **Dependency Injection**: Hilt
- **Asynchronous & Reactive**: Kotlin Coroutines & Flow
- **Local Persistence**: Room Database (Finalized Sessions), DataStore Preferences (Settings)
- **Media Playback**: AndroidX Media3 (ExoPlayer & MediaSessionService)
- **Scheduling**: Android AlarmManager & NotificationManager APIs
- **Build System**: Gradle 9.4.1, AGP 9.2.1, KSP, Kotlin Serialization

### Package Layout
```text
com.cydoniancitizen.mindora/
├── feature/
│   ├── practice/          # Main practice dashboard & navigation hub
│   ├── freemeditation/    # Free meditation timer & session controls
│   ├── breathing/         # Interactive breathing exercise & visualizer
│   ├── guidedmeditation/  # Guided audio session playback
│   ├── library/           # Meditation library & meditation detail
│   ├── whitenoise/        # Looping White Noise session
│   ├── pathdetail/        # Mindfulness path steps & step detail viewer
│   ├── history/           # Local session history logs & statistics
│   └── settings/          # Weekly goal, reminders, vibration, language & data export
├── core/
│   ├── content/           # Bundled JSON catalogue: library meditations & paths
│   ├── database/          # Room database, entities, DAOs, & migrations
│   ├── export/            # JSON export of preferences & sessions
│   ├── goal/              # Weekly goal management & progress derivation
│   ├── media/             # Media3 playback service for guided audio & White Noise
│   ├── preferences/       # DataStore preference repositories
│   ├── progress/          # User progress calculations
│   ├── reminder/          # Local alarm scheduling & notification receivers
│   └── session/           # Practice session runtime state management
├── navigation/            # Compose Navigation graph & destination routes
└── ui/                    # Material 3 theme & shared session components
```

---

## Build & Test

### Environment Requirements
- JDK 17 or newer (Java bytecode target is 17; Gradle 9.4.1 also runs on JDK 21)
- Android SDK 36 (compileSdk = 36, targetSdk = 36, minSdk = 33)
- Device or Emulator running Android 13 (API 33) or higher

### Gradle Commands

Using the checked-in Gradle wrapper:

```powershell
# Run unit tests
.\gradlew.bat test

# Run Android lint
.\gradlew.bat lint

# Build debug APK
.\gradlew.bat assembleDebug

# Build release APK (unsigned)
.\gradlew.bat assembleRelease
```

*(On macOS/Linux, replace `.\gradlew.bat` with `./gradlew`)*

> **Note**: Release builds use code shrinking (`isMinifyEnabled = true`, `isShrinkResources = true`). Release signing credentials are not stored in the repository; generated release artifacts are unsigned unless local signing parameters are configured.

---

## Known Limitations

- **Session Restoration**: Active free meditation and breathing sessions are not restored across process termination.
- **Inexact Alarms**: Local reminder delivery uses inexact scheduling to preserve battery and may be delayed by Android Doze mode or power management policies.
- **Guided Playback**: Runtime testing of guided audio playback requires approved bundled media, which is currently absent in the production catalogue.
- **Store Publishing**: Public store release requires secure signing keys and a hosted privacy policy URL.
- **Export Is Not a Restorable Backup**: Settings offers a JSON export written through the system document picker. It is a readable copy of preferences and finalized sessions; there is no import, so it cannot be loaded back into the app to restore a device. Session durations are exported in milliseconds, matching what Room stores, under export `format` version 2.

---

## Contributing

Contributions must align with Mindora's core product principles:
1. **Offline-First & Local Privacy**: No cloud sync, external network calls, remote telemetry, or user tracking.
2. **Calm & Non-Punitive**: No streak pressure, social comparisons, leaderboards, dark patterns, or gamification mechanics.
3. **Clean Code & Focus**: Small, coherent changes that satisfy existing requirements without speculative abstractions ("No AI slop").

Before submitting a change, run unit tests, lint checks, and assemble builds to ensure all verification checks pass.

---

## License

Mindora source code is licensed under the [Apache License 2.0](LICENSE). Creative media assets carry individual provenance and licensing as detailed in the attribution section.
