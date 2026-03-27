package io.j3mobile.network

import io.j3mobile.protocol.WsPush
import io.j3mobile.protocol.WsResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.JsonObject

interface BridgeTransport {
    val connectionState: StateFlow<ConnectionState>
    val pushEvents: SharedFlow<WsPush>

    fun connect(scope: CoroutineScope)

    suspend fun sendRequest(method: String, params: JsonObject): WsResponse

    fun disconnect()
}
