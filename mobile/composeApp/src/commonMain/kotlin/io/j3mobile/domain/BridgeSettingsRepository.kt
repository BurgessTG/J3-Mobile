package io.j3mobile.domain

import com.russhwolf.settings.Settings

class BridgeSettingsRepository(
    private val settings: Settings = Settings(),
) {
    fun load(): BridgeSettings? {
        val baseUrl = settings.getStringOrNull(KEY_BASE_URL)?.trim().orEmpty()
        val jwt = settings.getStringOrNull(KEY_JWT)?.trim().orEmpty()

        if (baseUrl.isBlank() || jwt.isBlank()) {
            return null
        }

        return BridgeSettings(baseUrl = baseUrl.trimEnd('/'), jwt = jwt)
    }

    fun save(value: BridgeSettings) {
        settings.putString(KEY_BASE_URL, value.baseUrl.trim().trimEnd('/'))
        settings.putString(KEY_JWT, value.jwt.trim())
    }

    fun clear() {
        settings.remove(KEY_BASE_URL)
        settings.remove(KEY_JWT)
    }

    fun hasValue(): Boolean = load() != null

    companion object {
        private const val KEY_BASE_URL = "bridge.baseUrl"
        private const val KEY_JWT = "bridge.jwt"
    }
}
