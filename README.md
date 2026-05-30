# EzMedicator

Android app for managing and being reminded to take your medication.

- All data is stored locally — no network connection required
- Data is encrypted at rest using `EncryptedSharedPreferences`
- Minimum Android version: 8.0 (API 26)

---

## Features

### Medications

- Add medications with a title and an optional icon
- Choose from a set of built-in icons (pill, capsule, bottle, and more) and pick a custom color for each
- Each medication can have multiple reminders

### Reminders

- Reminders are scheduled using standard cron expressions (5-field Unix format, e.g. `0 8 * * *` for daily at 08:00)
- When a reminder fires, a full-screen alarm is shown with sound and vibration (configurable per reminder)
- The next scheduled occurrence is shown on each reminder, including day of week, date, time, and a countdown (e.g. `Mon 02.06.2026 08:00 (in 1d 3h)`)

### Alarm screen

- Displays the medication name and a countdown until the reminder is automatically delayed
- User can dismiss or manually delay from the alarm screen
- If not acted upon within the configured timeout, the reminder is automatically delayed by the configured auto-delay duration and a silent info notification is sent

### Skip & Delay

- **Skip next occurrence**: marks the next scheduled alarm to be silently skipped. The skip is automatically cleared when the original scheduled time is reached, so subsequent alarms fire normally
- **Delay next**: postpones the next occurrence by a chosen number of minutes. Delay steps are configurable globally in Settings
- **Reset**: clears any active skip or delay, returning to the original schedule
- Skip and delay controls are available directly on the medication list, without needing to open the detail screen

### Per-reminder notification settings

- Enable/disable the reminder
- Auto-delay timeout (seconds before auto-delay kicks in)
- Auto-delay duration (minutes to delay by when timeout fires)
- Vibration on/off
- Sound selection (uses the system ringtone picker)

### Global settings

- Configure the selectable delay steps (shown in the delay picker)
- Set default values for timeout and auto-delay duration applied to new reminders

### Activity log

- Tracks all reminder-related events: triggered, dismissed, skipped, manually delayed, auto-delayed, and reset
- Accessible from the medication list via the top bar

---

## How to use

### Adding a medication

1. Tap **+** on the medication list screen
2. Enter a title
3. Optionally select an icon and color
4. Tap **Save**

### Adding a reminder

1. On the medication list, tap **Add Reminder** on a medication card, or open **Edit Reminders** and tap **+**
2. Enter a cron expression for the schedule (e.g. `0 8 * * *` = daily at 08:00, `30 12 * * 1-5` = weekdays at 12:30)
3. Adjust notification settings as needed
4. Tap **Save**

### Skipping or delaying the next occurrence

On the medication list, each reminder row shows:
- The next scheduled occurrence with countdown
- A **Skip** chip — tap to mark the next alarm as skipped; tap again or use **Reset** (↺) to undo
- A **Delay** button — opens a picker to postpone by a preset or custom number of minutes
- A **↺** reset button — appears when a skip or delay is active; clears it

### When an alarm fires

- The full-screen alarm shows the medication name and a countdown to auto-delay
- Tap **Dismiss** to acknowledge the reminder
- Tap **Delay** to postpone it

---

## Planned features

- Fix: "Skip next occurrence" button text wrapping in the reminder detail view
- App-level access protection (password, passkey, or fingerprint)
- Hide specific medications behind authorization
- Lock editing of specific or all medications and reminders without authorization

---

## Tech stack

Kotlin · Jetpack Compose · Room · Hilt · AlarmManager · EncryptedSharedPreferences · cron-utils
