package com.whats.notesapp.presentation.utils

import androidx.compose.runtime.Composable
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit


object DateFormatter {

    private val millisInMinute = TimeUnit.MINUTES.toMillis(1)
    private val millisInHour = TimeUnit.HOURS.toMillis(1)
    private val millisInDay = TimeUnit.DAYS.toMillis(1)

    private val formatter = SimpleDateFormat.getDateInstance(DateFormat.SHORT)

    fun formatCurrentDate(): String {
        return formatter.format(System.currentTimeMillis())
    }

    @Composable
    fun formateDataToString(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            // Меньше минуты
            diff < millisInMinute -> "Just now"

            // Меньше часа (в минутах)
            diff < millisInHour -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                "${minutes}m ago"
            }

            // Меньше суток (в часах)
            diff < millisInDay -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                "${hours}h ago"
            }

            // Больше днф
            else -> {
                formatter.format(timestamp)
            }
        }
    }
}