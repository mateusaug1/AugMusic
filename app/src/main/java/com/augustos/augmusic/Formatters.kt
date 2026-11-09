package com.augustos.augmusic

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun formatDuration(ms: Long): String {
    val total = ms.coerceAtLeast(0L) / 1000L
    val h = total / 3600L
    val m = (total % 3600L) / 60L
    val s = total % 60L
    return if (h > 0L) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.ROOT, "%d:%02d", m, s)
}

internal fun formatBytes(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0)
    else -> String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0))
}

/** MediaStore guarda DATE_ADDED / DATE_MODIFIED em segundos desde 1970. */
internal fun formatDateSeconds(seconds: Long): String =
    if (seconds <= 0L) "—"
    else SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(seconds * 1000L))

internal fun appVersionLabel(context: Context): String = try {
    @Suppress("DEPRECATION")
    val name = context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    // "12.0.0" -> "12.0"
    name.split('.').take(2).joinToString(".").ifBlank { name }
} catch (_: Exception) {
    "12.0"
}
