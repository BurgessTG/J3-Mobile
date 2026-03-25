package io.j3mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.j3mobile.network.BridgeWsClient
import io.j3mobile.protocol.ThreadId
import io.j3mobile.protocol.WsChannels
import io.j3mobile.protocol.WsMethods
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class TerminalViewModel(
    private val threadId: ThreadId,
    private val wsClient: BridgeWsClient,
) : ViewModel() {
    private val _output = MutableStateFlow("")
    val output: StateFlow<String> = _output.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    init {
        viewModelScope.launch {
            wsClient.pushEvents
                .filter { it.channel == WsChannels.TERMINAL_EVENT }
                .collect { push ->
                    val obj = push.data.jsonObject
                    val eventThreadId = obj["threadId"]?.jsonPrimitive?.contentOrNull ?: return@collect
                    if (eventThreadId != threadId.value) return@collect

                    when (obj["type"]?.jsonPrimitive?.contentOrNull) {
                        "output" -> {
                            val text = obj["data"]?.jsonPrimitive?.contentOrNull ?: return@collect
                            _output.value += text
                        }
                        "started" -> _isRunning.value = true
                        "exited" -> _isRunning.value = false
                        "cleared" -> _output.value = ""
                    }
                }
        }
    }

    fun sendInput(text: String) {
        viewModelScope.launch {
            wsClient.sendRequest(
                WsMethods.TERMINAL_WRITE,
                buildJsonObject {
                    put("threadId", JsonPrimitive(threadId.value))
                    put("data", JsonPrimitive(text))
                },
            )
        }
    }

    fun openTerminal(cwd: String) {
        viewModelScope.launch {
            wsClient.sendRequest(
                WsMethods.TERMINAL_OPEN,
                buildJsonObject {
                    put("threadId", JsonPrimitive(threadId.value))
                    put("cwd", JsonPrimitive(cwd))
                },
            )
        }
    }
}
