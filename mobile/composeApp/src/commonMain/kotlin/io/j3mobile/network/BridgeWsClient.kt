package io.j3mobile.network

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.HttpHeaders
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import io.j3mobile.protocol.*
import kotlin.time.Duration.Companion.milliseconds

class BridgeWsClient(
    private val baseUrl: String,
    private val token: String,
) : BridgeTransport {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient {
        install(WebSockets)
    }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _pushEvents = MutableSharedFlow<WsPush>(extraBufferCapacity = 64)
    override val pushEvents: SharedFlow<WsPush> = _pushEvents.asSharedFlow()

    private val counterMutex = Mutex()
    private var requestIdCounter = 0L

    private val pendingMutex = Mutex()
    private val pendingRequests = mutableMapOf<String, CompletableDeferred<WsResponse>>()

    private var session: DefaultClientWebSocketSession? = null
    private var connectionJob: Job? = null

    private val reconnectDelays = listOf(500L, 1000L, 2000L, 4000L, 8000L)
    private var lastSequence = 0

    /** Connect to the bridge WebSocket. */
    override fun connect(scope: CoroutineScope) {
        connectionJob = scope.launch {
            connectInternal()
        }
    }

    private suspend fun connectInternal() {
        _connectionState.value = ConnectionState.Connecting
        var attempt = 0

        while (true) {
            try {
                client.webSocket(
                    urlString = baseUrl.toWebSocketUrl("/ws"),
                    request = {
                        headers.append(HttpHeaders.Authorization, "Bearer $token")
                    },
                ) {
                    session = this
                    _connectionState.value = ConnectionState.Connected(sessionId = "active")
                    attempt = 0

                    // If reconnecting, replay missed events
                    if (lastSequence > 0) {
                        replayMissedEvents()
                    }

                    // Read loop
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            handleMessage(frame.readText())
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Connection lost — will retry below
            }

            session = null
            val delayMs = reconnectDelays[attempt.coerceAtMost(reconnectDelays.lastIndex)]
            _connectionState.value = ConnectionState.Reconnecting(attempt)
            attempt++
            delay(delayMs.milliseconds)
        }
    }

    private suspend fun handleMessage(text: String) {
        val element = json.parseToJsonElement(text).jsonObject

        // Check if it's a response (has "id" field and result/error)
        val id = element["id"]?.jsonPrimitive?.contentOrNull
        if (id != null && (element.containsKey("result") || element.containsKey("error"))) {
            val response = json.decodeFromJsonElement<WsResponse>(element)
            val deferred = pendingMutex.withLock { pendingRequests.remove(id) }
            deferred?.complete(response)
            return
        }

        // Check if it's a push (has "type": "push")
        val type = element["type"]?.jsonPrimitive?.contentOrNull
        if (type == "push") {
            val push = json.decodeFromJsonElement<WsPush>(element)
            lastSequence = maxOf(lastSequence, push.sequence)
            _pushEvents.tryEmit(push)
        }
    }

    /** Send an RPC request and wait for the response. */
    override suspend fun sendRequest(method: String, params: JsonObject): WsResponse {
        val id = counterMutex.withLock { (++requestIdCounter).toString() }
        val body = buildJsonObject {
            put("_tag", JsonPrimitive(method))
            params.forEach { (key, value) -> put(key, value) }
        }
        val request = WsRequest(id = id, body = body)
        val requestJson = json.encodeToString(WsRequest.serializer(), request)

        val deferred = CompletableDeferred<WsResponse>()
        pendingMutex.withLock { pendingRequests[id] = deferred }

        session?.send(Frame.Text(requestJson))
            ?: throw IllegalStateException("Not connected")

        return deferred.await()
    }

    private suspend fun replayMissedEvents() {
        val params = buildJsonObject {
            put("fromSequenceExclusive", JsonPrimitive(lastSequence))
        }
        sendRequest(WsMethods.ORCHESTRATION_REPLAY_EVENTS, params)
    }

    /** Disconnect and release resources. */
    override fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
        session = null
        _connectionState.value = ConnectionState.Disconnected
        pendingRequests.values.forEach { it.cancel() }
        pendingRequests.clear()
        client.close()
    }
}
