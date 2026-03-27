package io.j3mobile.di

import io.j3mobile.domain.PendingApproval
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
import kotlinx.coroutines.flow.collectLatest
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

    private val _pendingApproval = MutableStateFlow<PendingApproval?>(null)
    val pendingApproval: StateFlow<PendingApproval?> = _pendingApproval.asStateFlow()

    private var _wsClient: BridgeWsClient? = null
    private var _apiClient: BridgeApiClient? = null
    private var _sessionManager: SessionManager? = null
    private var _threadRepo: ThreadRepository? = null
    private var stateForwardJob: Job? = null
    private var approvalForwardJob: Job? = null

    val wsClient: BridgeWsClient? get() = _wsClient
    val threadRepo: ThreadRepository? get() = _threadRepo

    suspend fun connect(url: String, token: String, scope: CoroutineScope) {
        val normalizedUrl = url.trim().trimEnd('/')
        val normalizedToken = token.trim()

        require(normalizedUrl.isNotBlank()) { "Bridge URL is required" }
        require(normalizedToken.isNotBlank()) { "Auth token is required" }

        disconnect()
        _connectionState.value = ConnectionState.Connecting

        try {
            val api = BridgeApiClient(normalizedUrl, normalizedToken)
            val ws = BridgeWsClient(normalizedUrl, normalizedToken)
            val threadRepo = ThreadRepository(ws)
            val sessionManager = SessionManager(
                wsClient = ws,
                store = store,
                loadSnapshot = {
                    store.loadSnapshot(api.getSnapshot())
                },
            )

            _apiClient = api
            _wsClient = ws
            _threadRepo = threadRepo
            _sessionManager = sessionManager

            stateForwardJob = scope.launch {
                ws.connectionState.collectLatest { _connectionState.value = it }
            }
            approvalForwardJob = scope.launch {
                sessionManager.pendingApproval.collectLatest { _pendingApproval.value = it }
            }

            sessionManager.start(scope)
        } catch (e: Exception) {
            disconnect()
            _connectionState.value = ConnectionState.Error(e.message ?: "Failed to connect")
            throw e
        }
    }

    fun disconnect() {
        stateForwardJob?.cancel()
        stateForwardJob = null
        approvalForwardJob?.cancel()
        approvalForwardJob = null
        _sessionManager?.stop()
        _wsClient?.disconnect()
        _apiClient?.close()
        _wsClient = null
        _apiClient = null
        _sessionManager = null
        _threadRepo = null
        _pendingApproval.value = null
        _connectionState.value = ConnectionState.Disconnected
    }
}
