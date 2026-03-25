package io.j3mobile.viewmodel

import androidx.lifecycle.ViewModel
import io.j3mobile.domain.SessionManager
import io.j3mobile.network.ConnectionState
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(
    private val sessionManager: SessionManager,
) : ViewModel() {
    val connectionState: StateFlow<ConnectionState> = sessionManager.connectionState

    // Settings will be expanded later with:
    // - Bridge URL configuration
    // - Auth token management
    // - Default model selection
    // - Theme preferences
}
