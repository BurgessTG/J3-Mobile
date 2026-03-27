package io.j3mobile.domain

import io.j3mobile.network.BridgeApiClient
import io.j3mobile.network.BridgeTransport
import io.j3mobile.network.BridgeWsClient
import io.j3mobile.network.ConnectionState
import io.j3mobile.protocol.ApprovalRequestId
import io.j3mobile.protocol.ModelSelection
import io.j3mobile.protocol.OrchestrationReadModel
import io.j3mobile.protocol.ProjectId
import io.j3mobile.protocol.ThreadId
import io.j3mobile.protocol.WsChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AppContainer(
    private val bridgeSettingsRepository: BridgeSettingsRepository = BridgeSettingsRepository(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _bridgeSettings = MutableStateFlow<BridgeSettings?>(null)
    val bridgeSettings: StateFlow<BridgeSettings?> = _bridgeSettings.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _pendingApproval = MutableStateFlow<PendingApproval?>(null)
    val pendingApproval: StateFlow<PendingApproval?> = _pendingApproval.asStateFlow()

    val store = OrchestrationStore()

    private var runtime: Runtime? = null
    private var bootstrapJob: Job? = null

    fun bootstrap() {
        if (bootstrapJob != null) return
        bootstrapJob = scope.launch {
            val saved = bridgeSettingsRepository.load()
            _bridgeSettings.value = saved
            if (saved != null) {
                configureRuntime(saved)
            }
        }
    }

    fun saveBridgeSettings(baseUrl: String, jwt: String) {
        val normalized = BridgeSettings(
            baseUrl = baseUrl.trim().trimEnd('/'),
            jwt = jwt.trim(),
        )
        bridgeSettingsRepository.save(normalized)
        _bridgeSettings.value = normalized
        scope.launch {
            configureRuntime(normalized)
        }
    }

    fun clearBridgeSettings() {
        bridgeSettingsRepository.clear()
        _bridgeSettings.value = null
        disconnect()
    }

    fun retryConnection() {
        val settings = _bridgeSettings.value ?: return
        scope.launch {
            configureRuntime(settings)
        }
    }

    fun disconnect() {
        runtime?.close()
        runtime = null
        _connectionState.value = ConnectionState.Disconnected
        _pendingApproval.value = null
    }

    suspend fun createProject(
        title: String,
        workspaceRoot: String,
    ) {
        requireRuntime().threadRepository.createProject(
            id = ProjectId(generateId("project")),
            title = title,
            workspaceRoot = workspaceRoot,
        )
    }

    suspend fun createThread(
        projectId: ProjectId,
        title: String,
        modelSelection: ModelSelection,
        runtimeMode: String = "full-access",
    ): ThreadId {
        val runtime = requireRuntime()
        val threadId = runtime.threadRepository.newThreadId()
        runtime.threadRepository.createThread(
            id = threadId,
            projectId = projectId,
            title = title,
            modelSelection = modelSelection,
            runtimeMode = runtimeMode,
        )
        return threadId
    }

    suspend fun sendMessage(
        threadId: ThreadId,
        text: String,
        modelSelection: ModelSelection,
        runtimeMode: String = "full-access",
    ) {
        requireRuntime().threadRepository.sendMessage(
            threadId = threadId,
            text = text,
            modelSelection = modelSelection,
            runtimeMode = runtimeMode,
        )
    }

    suspend fun interruptTurn(threadId: ThreadId) {
        requireRuntime().threadRepository.interruptTurn(threadId)
    }

    suspend fun respondToApproval(
        threadId: ThreadId,
        requestId: ApprovalRequestId,
        decision: String,
    ) {
        requireRuntime().threadRepository.respondToApproval(threadId, requestId, decision)
    }

    suspend fun stopSession(threadId: ThreadId) {
        requireRuntime().threadRepository.stopSession(threadId)
    }

    suspend fun refreshSnapshot() {
        requireRuntime().sessionManager.requestSnapshotRefresh()
    }

    private suspend fun configureRuntime(settings: BridgeSettings) {
        runtime?.close()

        val apiClient = BridgeApiClient(settings.baseUrl, settings.jwt)
        val wsClient: BridgeTransport = BridgeWsClient(settings.baseUrl, settings.jwt)
        val threadRepository = ThreadRepository(wsClient)

        val newRuntime = Runtime(
            apiClient = apiClient,
            wsClient = wsClient,
            threadRepository = threadRepository,
        )

        runtime = newRuntime
        newRuntime.start()
    }

    private fun requireRuntime(): Runtime = runtime ?: error("Bridge settings are not configured")

    private fun generateId(prefix: String): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        val suffix = (1..12).map { chars.random() }.joinToString("")
        return "$prefix-$suffix"
    }

    private inner class Runtime(
        val apiClient: BridgeApiClient,
        val wsClient: BridgeTransport,
        val threadRepository: ThreadRepository,
    ) {
        private val runtimeJob = SupervisorJob()
        private val runtimeScope = CoroutineScope(scope.coroutineContext + runtimeJob)

        val sessionManager = SessionManager(
            wsClient = wsClient,
            store = store,
            loadSnapshot = {
                val snapshot: OrchestrationReadModel = apiClient.getSnapshot()
                store.loadSnapshot(snapshot)
            },
        )

        fun start() {
            runtimeScope.launch {
                wsClient.connectionState.collectLatest { state ->
                    _connectionState.value = state
                }
            }

            runtimeScope.launch {
                sessionManager.pendingApproval.collectLatest { approval ->
                    _pendingApproval.value = approval
                }
            }

            sessionManager.start(runtimeScope)
        }

        fun close() {
            sessionManager.stop()
            runtimeJob.cancel()
            wsClient.disconnect()
            apiClient.close()
        }
    }
}
