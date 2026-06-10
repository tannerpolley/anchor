package com.itsjeel01.remotevcsmanager.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TimeFormat {

    private val isoParser: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val shortFormatter: SimpleDateFormat = SimpleDateFormat("MMM d", Locale.US)
    private val timeFormatter: SimpleDateFormat = SimpleDateFormat("HH:mm", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }

    fun relative(iso: String): String = try {
        val d = isoParser.parse(iso.take(19)) ?: return iso
        val diff = Date().time - d.time
        val minutes = diff / 60000
        val hours = minutes / 60
        val days = hours / 24
        val weeks = days / 7
        when {
            minutes < 1 -> "now"
            minutes < 60 -> "${minutes}m"
            hours < 24 -> "${hours}h"
            days < 7 -> "${days}d"
            weeks < 5 -> "${weeks}w"
            else -> shortFormatter.format(d)
        }
    } catch (_: Exception) {
        iso
    }

    fun now(): String = timeFormatter.format(Date())
}
