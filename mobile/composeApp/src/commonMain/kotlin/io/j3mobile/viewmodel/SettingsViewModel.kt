package io.j3mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.j3mobile.di.ConnectionManager
import io.j3mobile.domain.BridgeSettings
import io.j3mobile.domain.BridgeSettingsRepository
import io.j3mobile.network.ConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val connectionManager: ConnectionManager,
    private val settingsRepository: BridgeSettingsRepository = BridgeSettingsRepository(),
) : ViewModel() {
    val connectionState: StateFlow<ConnectionState> = connectionManager.connectionState

    private val savedSettings = settingsRepository.load()

    private val _bridgeUrl = MutableStateFlow(savedSettings?.baseUrl ?: DEFAULT_BRIDGE_URL)
    val bridgeUrl: StateFlow<String> = _bridgeUrl.asStateFlow()

    private val _authToken = MutableStateFlow(savedSettings?.jwt.orEmpty())
    val authToken: StateFlow<String> = _authToken.asStateFlow()

    private val _hasSavedSettings = MutableStateFlow(savedSettings != null)
    val hasSavedSettings: StateFlow<Boolean> = _hasSavedSettings.asStateFlow()

    fun updateBridgeUrl(url: String) {
        _bridgeUrl.value = url
    }

    fun updateAuthToken(token: String) {
        _authToken.value = token
    }

    fun saveAndConnect() {
        val settings = BridgeSettings(
            baseUrl = _bridgeUrl.value.trim().trimEnd('/'),
            jwt = _authToken.value.trim(),
        )
        if (settings.baseUrl.isBlank() || settings.jwt.isBlank()) return

        settingsRepository.save(settings)
        _bridgeUrl.value = settings.baseUrl
        _authToken.value = settings.jwt
        _hasSavedSettings.value = true

        viewModelScope.launch {
            try {
                connectionManager.connect(
                    url = settings.baseUrl,
                    token = settings.jwt,
                    scope = viewModelScope,
                )
            } catch (_: Exception) {
                // Error is reflected via connectionState
            }
        }
    }

    fun clearSavedConnection() {
        settingsRepository.clear()
        connectionManager.disconnect()
        _bridgeUrl.value = DEFAULT_BRIDGE_URL
        _authToken.value = ""
        _hasSavedSettings.value = false
    }

    companion object {
        private const val DEFAULT_BRIDGE_URL = "http://127.0.0.1:8181"
    }
}
