# Mindora

Mindora is an open-source Android mindfulness app built for private, offline practice. It provides standalone free-meditation and breathing sessions, local session history, weekly goals, local reminders, and support for ordered mindfulness paths with bundled guided audio.

## Current status

Core MVP engineering is implemented. Production catalogue is currently empty, so no mindfulness paths or guided meditations are visible to production users. Standalone free meditation and breathing remain available. No placeholder catalogue or test audio is packaged in production.

Mindora has no accounts, backend, analytics, telemetry, advertising, remote configuration, or application networking. Session history is stored in Room; small preferences are stored in DataStore. Static path content is read from bundled JSON assets. Guided audio, when approved, must be bundled with the application.

## Build

Requirements:

- JDK 17
- Android SDK 36
- Android 13 (API 33) or newer device/emulator

Use checked-in Gradle wrapper:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:assembleDebug
```

On macOS/Linux, use corresponding `./gradlew` commands. Release verification uses `:app:testReleaseUnitTest`, `:app:lintRelease`, `:app:assembleRelease`, and `:app:bundleRelease`. The repository does not contain release-signing credentials; generated release artifacts are unsigned unless secure local signing is supplied externally.

## Architecture

Single Android application module uses Jetpack Compose, Material 3, Navigation Compose, Hilt, coroutines and Flow, Room, Preferences DataStore, Media3, and Android alarm/notification APIs. UI observes immutable ViewModel state. Room owns finalized session history; progress and weekly totals are derived. DataStore owns lightweight settings. MediaSessionService owns sole ExoPlayer and MediaSession.

## Known limits

- Active free-meditation and breathing sessions are not restored after process death.
- Reminder delivery is inexact and may be deferred by Android power management.
- Guided playback runtime testing requires approved bundled audio; none exists in production catalogue now.
- Public release still requires approved branding, secure signing, store metadata, and hosted privacy-policy URL.

## Contributing

Keep changes offline-only, privacy-respecting, accessible, and limited to current product scope. Do not add production media without verified redistribution terms and an entry in `THIRD_PARTY_ASSETS.md`. Run relevant unit tests, lint, instrumentation tests when device available, and debug/release builds before submitting changes.

Code is licensed under Apache License 2.0; see `LICENSE`. Creative media remains separately licensed and must carry explicit provenance.
