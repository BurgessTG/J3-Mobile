package io.j3mobile.ui.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.math.absoluteValue

fun formatRelativeTime(isoTimestamp: String): String {
    val instant = runCatching { Instant.parse(isoTimestamp) }.getOrNull() ?: return ""
    val now = Clock.System.now()
    val deltaSeconds = (now - instant).inWholeSeconds
    val future = deltaSeconds < 0
    val seconds = deltaSeconds.absoluteValue

    val valueAndUnit = when {
        seconds < 60 -> "${seconds.coerceAtLeast(1)}s"
        seconds < 3_600 -> "${seconds / 60}m"
        seconds < 86_400 -> "${seconds / 3_600}h"
        else -> "${seconds / 86_400}d"
    }

    return if (future) "in $valueAndUnit" else "$valueAndUnit ago"
}
