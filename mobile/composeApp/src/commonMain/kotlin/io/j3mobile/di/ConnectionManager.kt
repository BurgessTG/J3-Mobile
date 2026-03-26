package io.j3mobile.di

import io.j3mobile.domain.OrchestrationStore
import io.j3mobile.domain.SessionManager
import io.j3mobile.domain.ThreadRepository
import io.j3mobile.network.BridgeApiClient
import io.j3mobile.network.BridgeWsClient
import io.j3mobile.network.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Central dependency holder for all bridge connectivity.
 * The [store] is always available; network components are created
 * on-demand when [connect] is called and torn down on [disconnect].
 */
class ConnectionManager {
    val store = OrchestrationStore()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var _wsClient: BridgeWsClient? = null
    private var _apiClient: BridgeApiClient? = null
    private var _sessionManager: SessionManager? = null
    private var _threadRepo: ThreadRepository? = null
    private var stateForwardJob: Job? = null

    val wsClient: BridgeWsClient? get() = _wsClient
    val threadRepo: ThreadRepository? get() = _threadRepo

    suspend fun connect(url: String, token: String, scope: CoroutineScope) {
        disconnect()

        val api = BridgeApiClient(url, token)
        val ws = BridgeWsClient(url, token)
        _apiClient = api
        _wsClient = ws
        _threadRepo = ThreadRepository(ws)

        val sm = SessionManager(api, ws, store)
        _sessionManager = sm

        // Forward connection state from the WebSocket client
        stateForwardJob = scope.launch {
            ws.connectionState.collect { _connectionState.value = it }
        }

        sm.initialize(scope)
    }

    fun disconnect() {
        stateForwardJob?.cancel()
        _sessionManager?.disconnect()
        _apiClient?.close()
        _wsClient = null
        _apiClient = null
        _sessionManager = null
        _threadRepo = null
        _connectionState.value = ConnectionState.Disconnected
    }
}
