package io.j3mobile.network

import kotlin.test.Test
import kotlin.test.assertEquals

class BridgeUrlTest {
    @Test
    fun normalizesHttpAndHttpsUrlsToWebsocketEndpoints() {
        assertEquals("ws://127.0.0.1:8181/ws", "http://127.0.0.1:8181".toWebSocketUrl("/ws"))
        assertEquals("wss://example.com/ws", "https://example.com".toWebSocketUrl("/ws"))
        assertEquals("ws://localhost:8181/ws", "ws://localhost:8181".toWebSocketUrl("/ws"))
    }
}
