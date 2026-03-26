package io.j3mobile.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.j3mobile.di.ConnectionManager
import io.j3mobile.protocol.ModelSelection
import io.j3mobile.protocol.ProviderKind
import io.j3mobile.protocol.ThreadId
import io.j3mobile.ui.components.conversation.ConversationTopBar
import io.j3mobile.ui.components.conversation.MessageInputBar
import io.j3mobile.ui.components.conversation.MessageList
import io.j3mobile.viewmodel.ConversationViewModel
import org.koin.compose.koinInject

private val defaultModelSelection = ModelSelection(
    provider = ProviderKind.CLAUDE_AGENT,
    model = "claude-sonnet-4-20250514",
)

@Composable
fun ConversationScreen(
    threadId: ThreadId,
    onBack: () -> Unit = {},
    onTerminal: () -> Unit = {},
) {
    val connectionManager: ConnectionManager = koinInject()
    val threadRepo = connectionManager.threadRepo ?: return

    val vm = remember(threadId) {
        ConversationViewModel(threadId, connectionManager.store, threadRepo)
    }

    val thread by vm.thread.collectAsState()
    val messages by vm.messages.collectAsState()
    val isRunning by vm.isRunning.collectAsState()
    val activities by vm.activities.collectAsState()
    val checkpoints by vm.checkpoints.collectAsState()
    val pendingApproval by vm.pendingApproval.collectAsState()

    val modelSelection = thread?.modelSelection ?: defaultModelSelection

    Scaffold(
        topBar = {
            ConversationTopBar(
                title = thread?.title ?: "Conversation",
                isRunning = isRunning,
                onBack = onBack,
                onStopSession = { vm.stopSession() },
                onInterrupt = { vm.interruptTurn() },
                onTerminal = onTerminal,
            )
        },
        bottomBar = {
            MessageInputBar(
                isRunning = isRunning,
                onSend = { text -> vm.sendMessage(text, modelSelection) },
                onInterrupt = { vm.interruptTurn() },
                modifier = Modifier.imePadding(),
            )
        },
    ) { paddingValues ->
        MessageList(
            messages = messages,
            activities = activities,
            checkpoints = checkpoints,
            pendingApproval = pendingApproval,
            onApprove = { info -> vm.respondToApproval(info.requestId, "approved") },
            onReject = { info -> vm.respondToApproval(info.requestId, "rejected") },
            onRevertCheckpoint = { turnCount -> vm.revertCheckpoint(turnCount) },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        )
    }
}
