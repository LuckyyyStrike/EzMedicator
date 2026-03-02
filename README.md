# EzMedicator
Android app to remind the user on when to take his medication

## Paradigms

- All data is local. 
- No network connection needed.

## Planned features

- User can add 'medications'
-- Contain a title
-- User can add an image to better identify the medication
-- Each medication can have multiple 'reminders'
-- 'Reminders' are scheduled by crons
- 'Reminders' notify the user via notifications
-- Are scheduled via crons and trigger a notification 
-- User can discard the next scheduled reminder in case they took their medication before the reminder triggers.
-- User can discard the reminder when it triggered a notification
-- User can delay the scheduled reminder in case they know that they will not be able to take their medication on time.
-- User can delay the reminder when it triggered a notification in case they can not take their medication at the moment.
-- Reminders will automatically delay after a given timeout when they have not been discarded or delayed by the user.
-- 'Reminders' have 'notification settings' 
- 'Notification settings' determine how the user is to be notified
-- 'Timeout in seconds' determines, how long the notification will be active before it automatically delays
-- 'Vibration' determines if the device will vibrate.
-- 'Sound' determines the sound the device will play
- Data is encrypted 
