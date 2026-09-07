package com.example.movereminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object ReminderScheduler {

    private const val REQUEST_CODE = 1001

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            return am.canScheduleExactAlarms()
        }
        return true
    }

    /**
     * 计算下一次提醒时间。
     * fromMillis 为 null：初次开启（或开机后）时，从"现在"开始计算下一个时间点。
     * fromMillis 不为 null：上一次闹钟触发的时间，在此基础上加一个间隔。
     * 注意：本实现假设 开始时间 < 结束时间（同一天内），不支持跨零点的区间。
     */
    private fun computeNextTrigger(prefs: Prefs, fromMillis: Long?): Long {
        val now = Calendar.getInstance()
        val intervalMs = prefs.intervalMinutes.coerceAtLeast(1) * 60_000L

        fun rangeStart(base: Calendar): Calendar {
            val c = base.clone() as Calendar
            c.set(Calendar.HOUR_OF_DAY, prefs.startHour)
            c.set(Calendar.MINUTE, prefs.startMinute)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            return c
        }

        fun rangeEnd(base: Calendar): Calendar {
            val c = base.clone() as Calendar
            c.set(Calendar.HOUR_OF_DAY, prefs.endHour)
            c.set(Calendar.MINUTE, prefs.endMinute)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            return c
        }

        if (fromMillis == null) {
            val todayStart = rangeStart(now)
            val todayEnd = rangeEnd(now)
            return when {
                now.before(todayStart) -> todayStart.timeInMillis
                now.after(todayEnd) -> {
                    val c = todayStart.clone() as Calendar
                    c.add(Calendar.DAY_OF_YEAR, 1)
                    c.timeInMillis
                }
                else -> {
                    val candidateMillis = now.timeInMillis + intervalMs
                    if (candidateMillis > todayEnd.timeInMillis) {
                        val c = todayStart.clone() as Calendar
                        c.add(Calendar.DAY_OF_YEAR, 1)
                        c.timeInMillis
                    } else {
                        candidateMillis
                    }
                }
            }
        } else {
            val prevCal = Calendar.getInstance().apply { timeInMillis = fromMillis }
            val startForPrevDay = rangeStart(prevCal)
            val endForPrevDay = rangeEnd(prevCal)
            val candidateMillis = fromMillis + intervalMs
            return if (candidateMillis > endForPrevDay.timeInMillis) {
                val c = startForPrevDay.clone() as Calendar
                c.add(Calendar.DAY_OF_YEAR, 1)
                c.timeInMillis
            } else {
                candidateMillis
            }
        }
    }

    fun scheduleNext(context: Context, fromMillis: Long? = null) {
        val prefs = Prefs.load(context)
        if (!prefs.enabled) return

        val triggerAt = computeNextTrigger(prefs, fromMillis)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent(context))
        } catch (e: SecurityException) {
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent(context))
        }
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(context))
    }
}
