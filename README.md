# Mindora

Mindora is an open-source, offline-first Android mindfulness application built for calm, private practice. It provides standalone free meditation timers, guided breathing exercises, ordered mindfulness paths with bundled audio support, local practice session history, weekly goals, local reminders, and a modern Material 3 design system built with Jetpack Compose.

---

## Current Status

Core MVP engineering is implemented. The production catalogue is currently empty, meaning no third-party mindfulness paths or guided meditations are visible in production builds. Standalone free meditation and breathing sessions remain fully accessible. No placeholder catalogue or test audio is packaged in production.

Mindora operates entirely offline. It has no user accounts, cloud backend, analytics, telemetry, advertising, remote configuration, or network communications. Session history is stored locally in Room; small preferences and settings are stored locally in DataStore. Static path content is read from bundled JSON assets. Guided audio, when approved, is bundled directly with the application.

---

## Privacy Policy

Mindora is designed to operate strictly offline and respects user privacy by default.

### Data Collection & Storage
- **No Remote Telemetry**: The app does not create accounts, collect analytics, send crash reports, or transmit session and preference data to any server.
- **Local Persistence**: Finalized practice sessions are stored locally on the device using Room. Weekly goals, reminder preferences, theme settings, and default timer configurations are stored locally in Preferences DataStore.
- **No Cloud Sync or Backups**: Android cloud backup (`allowBackup = false`) and device-to-device data transfer exclusions are explicitly declared. Clearing app data or uninstalling Mindora permanently removes all local data.

### Permissions
- **Notifications**: Notification permission is requested only after the user explicitly enables local reminders. Android system alarm and notification managers handle local reminder delivery.
- **Foreground Services & Audio**: Foreground service and media playback permissions support continuous background audio playback for guided sessions. Notification permission is not required for guided playback.

### Scope & Policy URL
This privacy description covers Mindora application behavior. It does not govern third-party platform behavior (such as Android OS, device manufacturers, or app stores). Currently, no public privacy-policy URL is hosted; distribution channels requiring a hosted policy URL remain blocked until one is published.

---

## Third-Party Assets & Attribution

No third-party meditation recordings, background music, nature sounds, custom fonts, or external images are included in production builds.

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
- **Language**: Kotlin 2.1.0
- **UI Framework**: Jetpack Compose, Material 3, Navigation Compose
- **Dependency Injection**: Hilt
- **Asynchronous & Reactive**: Kotlin Coroutines & Flow
- **Local Persistence**: Room Database (Finalized Sessions), DataStore Preferences (Settings)
- **Media Playback**: AndroidX Media3 (ExoPlayer & MediaSessionService)
- **Scheduling**: Android AlarmManager & NotificationManager APIs
- **Build System**: Gradle 8.11.1, AGP 8.8.0, KSP, Kotlin Serialization

### Package Layout
```text
com.cydoniancitizen.mindora/
├── feature/
│   ├── practice/          # Main practice dashboard & navigation hub
│   ├── freemeditation/    # Free meditation timer & session controls
│   ├── breathing/         # Interactive breathing exercise & visualizer
│   ├── guidedmeditation/  # Guided audio session playback
│   ├── pathdetail/        # Mindfulness path steps & step detail viewer
│   ├── history/           # Local session history logs & statistics
│   └── settings/          # Local reminder settings & theme preferences
├── core/
│   ├── content/           # Bundled JSON path content loader
│   ├── database/          # Room database, entities, DAOs, & migrations
│   ├── goal/              # Weekly goal management & progress derivation
│   ├── media/             # ExoPlayer MediaSessionService & player binding
│   ├── preferences/       # DataStore preference repositories
│   ├── progress/          # User progress calculations
│   ├── reminder/          # Local alarm scheduling & notification receivers
│   └── session/           # Practice session runtime state management
├── navigation/            # Compose Navigation graph & destination routes
└── ui/                    # Shared Material 3 design system tokens & theme
```

---

## Build & Test

### Environment Requirements
- JDK 17
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
