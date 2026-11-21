package com.example.domain.engine

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val keyFormatter: ThreadLocal<SimpleDateFormat> = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }

    private val displayFormatter: ThreadLocal<SimpleDateFormat> = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat = SimpleDateFormat("dd MMM yyyy", Locale("es", "ES"))
    }

    private val timeFormatter: ThreadLocal<SimpleDateFormat> = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    }

    fun todayKey(): String = keyFormatter.get()!!.format(Date())

    fun dateKeyFromMillis(millis: Long): String = keyFormatter.get()!!.format(Date(millis))

    fun formatDisplayDate(dateKey: String): String {
        return try {
            val date = keyFormatter.get()!!.parse(dateKey) ?: return dateKey
            displayFormatter.get()!!.format(date)
        } catch (_: Exception) {
            dateKey
        }
    }

    fun formatTime(millis: Long): String = timeFormatter.get()!!.format(Date(millis))

    fun daysBetween(olderMillis: Long, newerMillis: Long): Int {
        if (olderMillis <= 0L) return 999
        val diff = newerMillis - olderMillis
        return (diff / (1000L * 60L * 60L * 24L)).toInt()
    }

    fun nextDateKey(dateKey: String): String {
        return try {
            val cal = Calendar.getInstance()
            cal.time = keyFormatter.get()!!.parse(dateKey) ?: Date()
            cal.add(Calendar.DAY_OF_MONTH, 1)
            keyFormatter.get()!!.format(cal.time)
        } catch (_: Exception) {
            todayKey()
        }
    }

    fun millisUntilNextMidnight(): Long {
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 10)
            set(Calendar.MILLISECOND, 0)
        }
        return (next.timeInMillis - now.timeInMillis).coerceAtLeast(60_000L)
    }

    fun monthTitle(year: Int, monthZeroBased: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val fmt = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
        return fmt.format(cal.time).replaceFirstChar { it.uppercase() }
    }

    fun monthDaysGrid(year: Int, monthZeroBased: Int): List<String?> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        // Monday-first grid (1=Mon .. 7=Sun)
        val dow = cal.get(Calendar.DAY_OF_WEEK) // Sun=1, Mon=2 .. Sat=7
        val leadingEmpty = if (dow == Calendar.SUNDAY) 6 else dow - Calendar.MONDAY
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val result = MutableList<String?>(leadingEmpty) { null }
        for (day in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            result.add(keyFormatter.get()!!.format(cal.time))
        }
        return result
    }
}
