package io.j3mobile.domain

import io.j3mobile.network.BridgeTransport
import io.j3mobile.network.ConnectionState
import io.j3mobile.protocol.ModelSelection
import io.j3mobile.protocol.ProjectId
import io.j3mobile.protocol.ProviderKind
import io.j3mobile.protocol.ThreadId
import io.j3mobile.protocol.WsPush
import io.j3mobile.protocol.WsResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThreadRepositoryTest {
    @Test
    fun createThreadSerializesDispatchCommand() = runTest {
        val transport = RecordingTransport()
        val repository = ThreadRepository(transport)

        repository.createThread(
            id = ThreadId("thread-1"),
            projectId = ProjectId("project-1"),
            title = "Discuss build fixes",
            modelSelection = ModelSelection(
                provider = ProviderKind.CODEX,
                model = "gpt-5",
            ),
        )

        assertEquals("orchestration.dispatchCommand", transport.method)
        val command = transport.params?.get("command")?.jsonObject ?: error("missing command")
        assertEquals("thread.create", command["type"]?.jsonPrimitive?.content)
        assertEquals("thread-1", command["threadId"]?.jsonPrimitive?.content)
        assertEquals("project-1", command["projectId"]?.jsonPrimitive?.content)
        assertEquals("Discuss build fixes", command["title"]?.jsonPrimitive?.content)
        assertTrue(command["modelSelection"] != null)
    }

    private class RecordingTransport : BridgeTransport {
        override val connectionState: StateFlow<ConnectionState> = MutableStateFlow(ConnectionState.Disconnected)
        override val pushEvents: SharedFlow<WsPush> = MutableSharedFlow()

        var method: String? = null
        var params: JsonObject? = null

        override fun connect(scope: CoroutineScope) = Unit

        override suspend fun sendRequest(method: String, params: JsonObject): WsResponse {
            this.method = method
            this.params = params
            return WsResponse(id = "1")
        }

        override fun disconnect() = Unit
    }
}
