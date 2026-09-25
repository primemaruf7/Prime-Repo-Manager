package com.example.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    fun formatRelativeTime(isoString: String?): String {
        if (isoString.isNullOrBlank()) return ""
        return try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = isoFormat.parse(isoString) ?: return isoString
            val now = System.currentTimeMillis()
            val diff = now - date.time

            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24
            val months = days / 30
            val years = days / 365

            when {
                seconds < 60 -> "just now"
                minutes < 60 -> "${minutes}m ago"
                hours < 24 -> "${hours}h ago"
                days == 1L -> "yesterday"
                days < 30 -> "${days}d ago"
                months < 12 -> "${months}mo ago"
                else -> "${years}y ago"
            }
        } catch (e: Exception) {
            isoString.take(10)
        }
    }

    fun formatDate(isoString: String?): String {
        if (isoString.isNullOrBlank()) return ""
        return try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = isoFormat.parse(isoString) ?: return isoString
            val outputFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            outputFormat.format(date)
        } catch (e: Exception) {
            isoString.take(10)
        }
    }
}
