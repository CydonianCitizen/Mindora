# Privacy

Mindora is designed to operate offline. The app does not create an account, include application networking, or send analytics, telemetry, crash reports, advertising data, session data, or preferences to developer-operated services.

Finalized practice sessions are stored locally in Room. Goals, reminder settings, and other small preferences are stored locally in DataStore. Android backup is disabled, with backup and device-transfer exclusions also declared. Clearing app data or uninstalling Mindora removes local Mindora data subject to Android platform behavior.

Notification permission is requested only after the user explicitly enables reminders. Android system alarm and notification services manage local reminder scheduling and delivery. Foreground-service and media-playback permissions support bundled guided audio. Reminder permission is not required for guided playback.

This document describes Mindora application behavior. It does not claim Android, device manufacturer, app store, or other platform components collect no data. It is not legal advice.

No public privacy-policy URL is currently recorded. Distribution channels requiring hosted policy URL remain blocked until project publishes one.
