package com.progressvision.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Time {
    private val dateFmt = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))
    private val timeFmt = SimpleDateFormat("HH:mm", Locale("ru"))
    private val dayLabelFmt = SimpleDateFormat("dd MMM", Locale("ru"))
    private val dateTimeFmt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru"))

    fun formatDate(ms: Long): String = dateFmt.format(Date(ms))
    fun formatTime(ms: Long): String = timeFmt.format(Date(ms))
    fun formatDayLabel(ms: Long): String = dayLabelFmt.format(Date(ms))
    fun formatDateTime(ms: Long): String = dateTimeFmt.format(Date(ms))

    fun parseDateTime(value: String): Long? {
        val trimmed = value.trim().ifBlank { return null }
        return try {
            dateTimeFmt.parse(trimmed)?.time
        } catch (_: Exception) {
            try {
                dateFmt.parse(trimmed)?.time
            } catch (_: Exception) {
                null
            }
        }
    }

    fun startOfDay(ms: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = ms
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun endOfDay(ms: Long = System.currentTimeMillis()): Long =
        startOfDay(ms) + 24L * 3_600_000L

    fun startOfWeek(ms: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance(Locale("ru")).apply {
            firstDayOfWeek = Calendar.MONDAY
            timeInMillis = ms
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
        return cal.timeInMillis
    }

    fun daysAgo(days: Int): Long = startOfDay() - days * 24L * 3_600_000L

    fun formatDuration(sec: Long): String {
        if (sec <= 0) return "0 сек"
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        return when {
            h > 0 && m > 0 -> "${h} ч ${m} мин"
            h > 0 -> "${h} ч"
            m > 0 && s > 0 -> "${m} мин ${s} сек"
            m > 0 -> "${m} мин"
            else -> "${s} сек"
        }
    }

    fun formatStopwatch(sec: Long): String {
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s)
        else "%02d:%02d".format(m, s)
    }

    init {
        dateFmt.timeZone = TimeZone.getDefault()
        timeFmt.timeZone = TimeZone.getDefault()
        dayLabelFmt.timeZone = TimeZone.getDefault()
        dateTimeFmt.timeZone = TimeZone.getDefault()
    }
}
