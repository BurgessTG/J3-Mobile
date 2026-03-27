package io.j3mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.j3mobile.di.ConnectionManager
import io.j3mobile.protocol.ApprovalRequestId
import io.j3mobile.protocol.ModelSelection
import io.j3mobile.protocol.OrchestrationCheckpointSummary
import io.j3mobile.protocol.OrchestrationMessage
import io.j3mobile.protocol.OrchestrationThread
import io.j3mobile.protocol.OrchestrationThreadActivity
import io.j3mobile.protocol.ThreadId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Lightweight holder for a pending tool-approval request. */
data class PendingApprovalInfo(
    val requestId: ApprovalRequestId,
    val kind: String,
    val summary: String,
)

class ConversationViewModel(
    private val threadId: ThreadId,
    private val connectionManager: ConnectionManager,
) : ViewModel() {
    private val store = connectionManager.store

    val thread: StateFlow<OrchestrationThread?> =
        store.threadById(threadId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val messages: StateFlow<List<OrchestrationMessage>> =
        thread.map { it?.messages.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isRunning: StateFlow<Boolean> =
        thread.map { it?.latestTurn?.state == "running" }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val activities: StateFlow<List<OrchestrationThreadActivity>> =
        thread.map { it?.activities.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val checkpoints: StateFlow<List<OrchestrationCheckpointSummary>> =
        thread.map { it?.checkpoints.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** The most recent unresolved approval-request activity, if any. */
    val pendingApproval: StateFlow<PendingApprovalInfo?> =
        combine(activities, connectionManager.pendingApproval) { list, pending ->
            if (pending?.threadId != threadId) {
                null
            } else {
                val summary = list.lastOrNull { it.kind.contains("approval", ignoreCase = true) }?.summary
                    ?: "Approval required"
                PendingApprovalInfo(
                    requestId = pending.requestId,
                    kind = pending.sourceEventType,
                    summary = summary,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun sendMessage(text: String, modelSelection: ModelSelection) {
        viewModelScope.launch {
            connectionManager.threadRepo?.sendMessage(threadId, text, modelSelection)
        }
    }

    fun interruptTurn() {
        viewModelScope.launch {
            connectionManager.threadRepo?.interruptTurn(threadId)
        }
    }

    fun respondToApproval(requestId: ApprovalRequestId, decision: String) {
        viewModelScope.launch {
            connectionManager.threadRepo?.respondToApproval(threadId, requestId, decision)
        }
    }

    fun stopSession() {
        viewModelScope.launch {
            connectionManager.threadRepo?.stopSession(threadId)
        }
    }

    fun revertCheckpoint(turnCount: Int) {
        viewModelScope.launch {
            connectionManager.threadRepo?.revertCheckpoint(threadId, turnCount)
        }
    }
}
