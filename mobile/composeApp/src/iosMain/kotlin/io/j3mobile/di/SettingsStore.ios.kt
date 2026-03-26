package io.j3mobile.di

import platform.Foundation.NSUserDefaults

actual class SettingsStore actual constructor() {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun getString(key: String, default: String): String {
        return defaults.stringForKey(key) ?: default
    }

    actual fun putString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }
}
