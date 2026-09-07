package com.example.movereminder

import android.content.Context

data class Prefs(
    val enabled: Boolean,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val intervalMinutes: Int
) {
    companion object {
        private const val FILE = "reminder_prefs"

        fun load(context: Context): Prefs {
            val sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            return Prefs(
                enabled = sp.getBoolean("enabled", false),
                startHour = sp.getInt("startHour", 9),
                startMinute = sp.getInt("startMinute", 0),
                endHour = sp.getInt("endHour", 18),
                endMinute = sp.getInt("endMinute", 0),
                intervalMinutes = sp.getInt("intervalMinutes", 40)
            )
        }

        fun save(context: Context, prefs: Prefs) {
            val sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            sp.edit()
                .putBoolean("enabled", prefs.enabled)
                .putInt("startHour", prefs.startHour)
                .putInt("startMinute", prefs.startMinute)
                .putInt("endHour", prefs.endHour)
                .putInt("endMinute", prefs.endMinute)
                .putInt("intervalMinutes", prefs.intervalMinutes)
                .apply()
        }
    }
}
