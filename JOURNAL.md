# Development Journal

## 2026-06-30

### Bug fix: Reset from medication list not cancelling snooze alarm

**Problem:** When a reminder was auto-delayed (e.g. original 9 PM → snoozed to 10 PM) and the user pressed Reset on the medication list screen, the alarm still fired at 10 PM. Pressing Reset on the detail screen worked correctly.

**Root cause:** `MedicationListViewModel.resetNext()` called `reminderScheduler.schedule()` without first calling `reminderScheduler.cancel()`. Scheduling only updates the regular alarm PendingIntent (request code `reminderId`), leaving the snooze PendingIntent (request code `reminderId + 100_000`) live in AlarmManager. `MedicationDetailViewModel.resetNext()` already had the `cancel()` call — the list VM was missing it.

**Fix:** Added `reminderScheduler.cancel(updated.id)` before `reminderScheduler.schedule(updated)` in `MedicationListViewModel.resetNext()`.

---

### Feature: Scheduled alarms overview screen

**Motivation:** Wanted a diagnostic view showing what AlarmManager actually has registered, as opposed to what the DB thinks should be scheduled.

**Approach:** AlarmManager has no list API, so we probe each known reminder's PendingIntents using `FLAG_NO_CREATE`. Added `ReminderScheduler.isScheduled(reminderId, isSnooze)` as the probe hook. The ViewModel combines reminder/medication data from the repository with these live AlarmManager checks.

**What's shown per reminder:**
- Medication name + cron expression
- Regular alarm: whether the standard next-occurrence PendingIntent is registered
- Snooze alarm: whether the snooze PendingIntent is registered, plus the fire time if so

**Entry point:** Settings screen → "Scheduled Alarms" button (below Activity Log).

---

### Housekeeping

- Added `CLAUDE.md` with build commands and architecture overview for future Claude Code sessions.
