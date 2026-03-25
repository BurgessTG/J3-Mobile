package io.j3mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.j3mobile.network.BridgeWsClient
import io.j3mobile.protocol.GitBranch
import io.j3mobile.protocol.GitListBranchesResult
import io.j3mobile.protocol.GitStatusResult
import io.j3mobile.protocol.WsMethods
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class GitStatusViewModel(
    private val wsClient: BridgeWsClient,
) : ViewModel() {
    private val json = Json { ignoreUnknownKeys = true }

    private val _status = MutableStateFlow<GitStatusResult?>(null)
    val status: StateFlow<GitStatusResult?> = _status.asStateFlow()

    private val _branches = MutableStateFlow<List<GitBranch>>(emptyList())
    val branches: StateFlow<List<GitBranch>> = _branches.asStateFlow()

    fun loadStatus(cwd: String) {
        viewModelScope.launch {
            val response = wsClient.sendRequest(
                WsMethods.GIT_STATUS,
                buildJsonObject { put("cwd", JsonPrimitive(cwd)) },
            )
            response.result?.let {
                _status.value = json.decodeFromJsonElement(GitStatusResult.serializer(), it)
            }
        }
    }

    fun loadBranches(cwd: String) {
        viewModelScope.launch {
            val response = wsClient.sendRequest(
                WsMethods.GIT_LIST_BRANCHES,
                buildJsonObject { put("cwd", JsonPrimitive(cwd)) },
            )
            response.result?.let {
                val result = json.decodeFromJsonElement(GitListBranchesResult.serializer(), it)
                _branches.value = result.branches
            }
        }
    }

    fun pull(cwd: String) {
        viewModelScope.launch {
            wsClient.sendRequest(
                WsMethods.GIT_PULL,
                buildJsonObject { put("cwd", JsonPrimitive(cwd)) },
            )
            loadStatus(cwd) // refresh after pull
        }
    }
}
