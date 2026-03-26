package io.j3mobile.di

import android.content.Context
import android.content.SharedPreferences

actual class SettingsStore actual constructor() {
    // On Android, we use a global app context approach.
    // In production, inject Context via Koin; for now, use in-memory fallback.
    private val map = mutableMapOf<String, String>()

    actual fun getString(key: String, default: String): String {
        return map[key] ?: default
    }

    actual fun putString(key: String, value: String) {
        map[key] = value
    }
}
