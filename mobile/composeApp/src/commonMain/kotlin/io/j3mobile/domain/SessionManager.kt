package io.j3mobile.domain

import io.j3mobile.network.BridgeTransport
import io.j3mobile.network.ConnectionState
import io.j3mobile.protocol.OrchestrationEvent
import io.j3mobile.protocol.ThreadId
import io.j3mobile.protocol.WsChannels
import io.j3mobile.protocol.WsPush
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.coroutines.FlowPreview
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
class SessionManager(
    private val wsClient: BridgeTransport,
    private val store: OrchestrationStore,
    private val loadSnapshot: suspend () -> Unit,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val refreshRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    private val _pendingApproval = MutableStateFlow<PendingApproval?>(null)
    val pendingApproval: StateFlow<PendingApproval?> = _pendingApproval.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = wsClient.connectionState
    private var started = false
    private var refreshJob: Job? = null

    fun start(scope: CoroutineScope) {
        if (started) return
        started = true

        wsClient.connect(scope)

        scope.launch {
            connectionState.collectLatest { state ->
                if (state is ConnectionState.Connected) {
                    try {
                        loadSnapshot()
                    } catch (e: Exception) {
                        println("SessionManager initial snapshot load failed: ${e.message}")
                    }
                }
            }
        }

        refreshJob = scope.launch {
            refreshRequests
                .debounce(250.milliseconds)
                .collectLatest {
                    try {
                        loadSnapshot()
                    } catch (e: Exception) {
                        println("SessionManager refresh snapshot load failed: ${e.message}")
                    }
                }
        }

        scope.launch {
            wsClient.pushEvents.collect { push ->
                handlePush(push)
            }
        }
    }

    fun requestSnapshotRefresh() {
        refreshRequests.tryEmit(Unit)
    }

    private fun handlePush(push: WsPush) {
        when (push.channel) {
            WsChannels.ORCHESTRATION_DOMAIN_EVENT -> {
                try {
                    val event = json.decodeFromJsonElement(
                        OrchestrationEvent.serializer(),
                        push.data,
                    )
                    if (event.metadata.requestId != null) {
                        _pendingApproval.value = PendingApproval(
                            threadId = ThreadId(event.aggregateId),
                            requestId = event.metadata.requestId,
                            sourceEventType = event.type,
                        )
                    }
                    store.applyEventRefreshOnly()
                    requestSnapshotRefresh()
                } catch (e: Exception) {
                    println("SessionManager push handling failed: ${e.message}")
                }
            }
            // Other channels can be handled here
        }
    }

    fun stop() {
        refreshJob?.cancel()
        refreshJob = null
        started = false
        _pendingApproval.value = null
    }
}
