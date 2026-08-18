package com.messledger.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun today(): String = dateFormat.format(Date())
    
    fun yesterday(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        return dateFormat.format(calendar.time)
    }

    fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000} minutes ago"
            diff < 86400_000 -> "${diff / 3600_000} hours ago"
            else -> "${diff / 86400_000} days ago"
        }
    }
}
