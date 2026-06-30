# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build (assembles debug and release, runs lint and unit tests)
./gradlew build

# Assemble only
./gradlew assembleDebug
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run a single test class
./gradlew test --tests "de.ingomohrmann.ezmedicator.ExampleUnitTest"

# Install on connected device/emulator
./gradlew installDebug
```

CI runs `./gradlew build` on every push/PR to `main`.

## Architecture

Single-module Android app (`app/`). minSdk 26, targetSdk 35, written entirely in Kotlin.

**Layer structure:**

- `data/database/` — Room entities (`Medication`, `Reminder`, `LogEntry`) and DAOs. The database is `ezmedicator.db` (version 5). Migrations live in `di/AppModule.kt` — add new ones there when the schema changes.
- `data/repository/` — Repositories wrapping DAOs. `AppSettingsRepository` wraps `EncryptedSharedPreferences` (not Room) for global settings (delay steps, default timeout, default auto-delay).
- `domain/` — Pure logic: `CronHelper` (cron parsing/description/annotation via cron-utils, UNIX 5-field format), `ReminderScheduler` (schedules/cancels `AlarmManager` exact alarms).
- `notification/` — `NotificationHelper` builds and shows notifications. Two channels: `medication_reminders_v3` (alarm, silent — `AlarmActivity` handles audio/vibration directly) and `medication_info` (silent info channel for auto-delay confirmations). **When changing channel audio/vibration settings, bump the channel ID suffix** — Android ignores `createNotificationChannel()` for existing IDs.
- `receiver/` — `AlarmReceiver` (handles fired alarms: skip logic, calls `NotificationHelper`, schedules timeout alarm), `NotificationActionReceiver` (dismiss/delay/timeout actions from notification), `BootReceiver` (reschedules all alarms after reboot).
- `ui/` — Jetpack Compose screens with Hilt-injected ViewModels. Navigation is in `ui/navigation/NavGraph.kt`. Screens: `MedicationList`, `MedicationDetail`, `MedicationEdit`, `ReminderEdit`, `Settings`, `Log`, plus `AlarmActivity` (shown over lock screen).
- `widget/` — Glance app widget (`MedicationWidget`). Accesses repositories via a Hilt `@EntryPoint` (`WidgetEntryPoint`) since Glance doesn't support `@AndroidEntryPoint`. Widget shows up to 5 upcoming reminders with Skip and Delay buttons.
- `di/AppModule.kt` — Single Hilt module. Provides Room DB, DAOs, and `EncryptedSharedPreferences`.

**Alarm flow:**

1. `ReminderScheduler.schedule()` sets an exact `AlarmManager` alarm (or `setAndAllowWhileIdle` if exact alarm permission is denied).
2. `AlarmReceiver.onReceive()` fires: if `skipNextOccurrence` is set, silently clears it and reschedules; otherwise calls `NotificationHelper.showReminder()` + schedules a timeout alarm.
3. `NotificationHelper` fires a high-priority notification with a `fullScreenIntent` pointing to `AlarmActivity`.
4. `AlarmActivity` plays sound/vibration itself (the notification channel is silent so per-reminder sound URIs work).
5. Timeout fires into `NotificationActionReceiver` with `ACTION_TIMEOUT`, which auto-delays by calling `ReminderScheduler.scheduleSnooze()`.

**Snooze/skip state** is stored on `Reminder.snoozedUntil` (epoch millis) and `Reminder.skipNextOccurrence`. `ReminderScheduler.nextTriggerMillis()` checks `snoozedUntil` first, then falls back to the cron next execution. When a skip is set, the alarm still fires at the original cron time so `AlarmReceiver` can clear the flag — the UI independently shows `secondNextExecution` as the user-visible "next time."

**PendingIntent request codes** — must not collide:
- Regular alarm: `reminderId.toInt()`
- Snooze alarm: `reminderId + 100_000`
- Timeout alarm: `reminderId + 200_000`
- Full-screen intent: `notifId + 300_000`
- Auto-delayed notification: `reminderId + 500_000`
- Dismiss action: `notifId`
- Delay action: `notifId + 50_000`
