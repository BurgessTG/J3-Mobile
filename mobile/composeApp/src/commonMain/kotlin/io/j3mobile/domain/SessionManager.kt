package io.j3mobile.domain

import io.j3mobile.network.BridgeApiClient
import io.j3mobile.network.BridgeWsClient
import io.j3mobile.network.ConnectionState
import io.j3mobile.protocol.OrchestrationEvent
import io.j3mobile.protocol.WsChannels
import io.j3mobile.protocol.WsPush
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class SessionManager(
    private val apiClient: BridgeApiClient,
    private val wsClient: BridgeWsClient,
    private val store: OrchestrationStore,
) {
    private val json = Json { ignoreUnknownKeys = true }

    val connectionState: StateFlow<ConnectionState> = wsClient.connectionState

    /** Initialize: connect WS, load snapshot, subscribe to events. */
    suspend fun initialize(scope: CoroutineScope) {
        // Connect WebSocket
        wsClient.connect(scope)

        // Wait for connection
        wsClient.connectionState.first { it is ConnectionState.Connected }

        // Load initial snapshot
        loadSnapshot()

        // Subscribe to domain events
        scope.launch {
            wsClient.pushEvents.collect { push ->
                handlePush(push)
            }
        }
    }

    private suspend fun loadSnapshot() {
        try {
            val snapshot = apiClient.getSnapshot()
            store.loadSnapshot(snapshot)
        } catch (_: Exception) {
            // Log error, will retry on reconnect
        }
    }

    private fun handlePush(push: WsPush) {
        when (push.channel) {
            WsChannels.ORCHESTRATION_DOMAIN_EVENT -> {
                try {
                    val event = json.decodeFromJsonElement(
                        OrchestrationEvent.serializer(),
                        push.data,
                    )
                    store.applyEvent(event)
                } catch (_: Exception) {
                    // Log deserialization error
                }
            }
            // Other channels can be handled here
        }
    }

    fun disconnect() {
        wsClient.disconnect()
    }
}
