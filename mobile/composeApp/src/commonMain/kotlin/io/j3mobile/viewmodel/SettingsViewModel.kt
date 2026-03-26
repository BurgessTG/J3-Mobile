package io.j3mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.j3mobile.di.ConnectionManager
import io.j3mobile.network.ConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val connectionManager: ConnectionManager,
) : ViewModel() {
    val connectionState: StateFlow<ConnectionState> = connectionManager.connectionState

    private val _bridgeUrl = MutableStateFlow("http://localhost:4080")
    val bridgeUrl: StateFlow<String> = _bridgeUrl.asStateFlow()

    private val _authToken = MutableStateFlow("")
    val authToken: StateFlow<String> = _authToken.asStateFlow()

    fun updateBridgeUrl(url: String) {
        _bridgeUrl.value = url
    }

    fun updateAuthToken(token: String) {
        _authToken.value = token
    }

    fun connect() {
        viewModelScope.launch {
            try {
                connectionManager.connect(
                    url = _bridgeUrl.value,
                    token = _authToken.value,
                    scope = viewModelScope,
                )
            } catch (_: Exception) {
                // Error is reflected via connectionState
            }
        }
    }

    fun disconnect() {
        connectionManager.disconnect()
    }
}
