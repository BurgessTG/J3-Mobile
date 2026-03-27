package io.j3mobile.di

/**
 * Platform-specific key-value store for persisting settings
 * (bridge URL, auth token, etc.) across app launches.
 */
expect class SettingsStore() {
    fun getString(key: String, default: String): String
    fun putString(key: String, value: String)
}

object SettingsKeys {
    const val BRIDGE_URL = "bridge_url"
    const val AUTH_TOKEN = "auth_token"
}
