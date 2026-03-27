package io.j3mobile.domain

import io.j3mobile.network.BridgeTransport
import io.j3mobile.network.ConnectionState
import io.j3mobile.protocol.OrchestrationReadModel
import io.j3mobile.protocol.WsChannels
import io.j3mobile.protocol.WsPush
import io.j3mobile.protocol.WsResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SessionManagerTest {
    @Test
    fun refreshesSnapshotOnConnectAndPush() = runTest {
        val transport = FakeTransport()
        var snapshotLoads = 0
        val store = OrchestrationStore()
        val manager = SessionManager(
            wsClient = transport,
            store = store,
            loadSnapshot = {
                snapshotLoads++
                store.loadSnapshot(
                    OrchestrationReadModel(
                        snapshotSequence = snapshotLoads,
                        projects = emptyList(),
                        threads = emptyList(),
                        updatedAt = "2026-03-25T00:00:00Z",
                    ),
                )
            },
        )

        manager.start(backgroundScope)
        transport.markConnected()
        manager.requestSnapshotRefresh()
        advanceTimeBy(300)
        advanceUntilIdle()
        val loadsAfterInitialRefresh = snapshotLoads
        assertTrue(loadsAfterInitialRefresh >= 1)

        transport.pushEvents.emit(
            WsPush(
                type = "push",
                sequence = 3,
                channel = WsChannels.ORCHESTRATION_DOMAIN_EVENT,
                data = buildJsonObject {
                    put("sequence", JsonPrimitive(3))
                    put("eventId", JsonPrimitive("evt-1"))
                    put("type", JsonPrimitive("thread.approval.requested"))
                    put("aggregateKind", JsonPrimitive("thread"))
                    put("aggregateId", JsonPrimitive("thread-1"))
                    put("occurredAt", JsonPrimitive("2026-03-25T00:00:00Z"))
                    put("metadata", buildJsonObject {
                        put("requestId", JsonPrimitive("approval-1"))
                    })
                },
            ),
        )

        advanceTimeBy(300)
        advanceUntilIdle()

        assertTrue(snapshotLoads > loadsAfterInitialRefresh)
        assertEquals("approval-1", manager.pendingApproval.value?.requestId?.value)
    }

    private class FakeTransport : BridgeTransport {
        override val connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
        override val pushEvents = MutableSharedFlow<WsPush>(extraBufferCapacity = 16)

        override fun connect(scope: CoroutineScope) = Unit

        fun markConnected() {
            connectionState.value = ConnectionState.Connected(sessionId = "test-session")
        }

        override suspend fun sendRequest(method: String, params: kotlinx.serialization.json.JsonObject): WsResponse {
            return WsResponse(id = "1")
        }

        override fun disconnect() {
            connectionState.value = ConnectionState.Disconnected
        }
    }
}
