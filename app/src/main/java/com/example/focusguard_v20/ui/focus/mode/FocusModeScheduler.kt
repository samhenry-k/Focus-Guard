package com.example.focusguard_v20.ui.focus.mode

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

internal object FocusModeScheduler {
    fun scheduleEndOfOneTimeSession(
        context: Context,
        minutesFromNow: Int,
    ) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + (minutesFromNow.coerceAtLeast(1) * 60_000L)

        val pi =
            PendingIntent.getBroadcast(
                context,
                RequestCodes.EndOneTime,
                Intent(context, FocusModeAlarmReceiver::class.java).setAction(FocusModeAlarmReceiver.Actions.EndOneTimeSession),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        scheduleAt(am, triggerAt, pi)
    }

    fun scheduleDailyAutoWindow(
        context: Context,
        fromMinutesOfDay: Int,
        toMinutesOfDay: Int,
    ) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val startAt = nextTriggerAtMillis(fromMinutesOfDay)
        val endAt = nextTriggerAtMillis(toMinutesOfDay)

        val startPi =
            PendingIntent.getBroadcast(
                context,
                RequestCodes.AutoStart,
                Intent(context, FocusModeAlarmReceiver::class.java).setAction(FocusModeAlarmReceiver.Actions.AutoStart),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val endPi =
            PendingIntent.getBroadcast(
                context,
                RequestCodes.AutoEnd,
                Intent(context, FocusModeAlarmReceiver::class.java).setAction(FocusModeAlarmReceiver.Actions.AutoEnd),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        // Use repeating inexact alarms for reliability without exact-alarm permission prompts.
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, startAt, AlarmManager.INTERVAL_DAY, startPi)
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, endAt, AlarmManager.INTERVAL_DAY, endPi)
    }

    private fun scheduleAt(am: AlarmManager, triggerAtMillis: Long, pi: PendingIntent) {
        if (android.os.Build.VERSION.SDK_INT >= 31 && am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        } else {
            am.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    private fun nextTriggerAtMillis(minutesOfDay: Int): Long {
        val now = Calendar.getInstance()
        val cal = Calendar.getInstance()
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.set(Calendar.HOUR_OF_DAY, (minutesOfDay / 60).coerceIn(0, 23))
        cal.set(Calendar.MINUTE, (minutesOfDay % 60).coerceIn(0, 59))
        if (cal.timeInMillis <= now.timeInMillis) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    private object RequestCodes {
        const val EndOneTime = 7101
        const val AutoStart = 7102
        const val AutoEnd = 7103
    }
}

