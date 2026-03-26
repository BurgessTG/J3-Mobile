package io.j3mobile.network

internal fun String.toWebSocketUrl(path: String): String {
    val normalizedPath = if (path.startsWith("/")) path else "/$path"

    return when {
        startsWith("https://") -> replaceFirst("https://", "wss://").trimEnd('/') + normalizedPath
        startsWith("http://") -> replaceFirst("http://", "ws://").trimEnd('/') + normalizedPath
        startsWith("wss://") || startsWith("ws://") -> trimEnd('/') + normalizedPath
        else -> "ws://$this".trimEnd('/') + normalizedPath
    }
}
