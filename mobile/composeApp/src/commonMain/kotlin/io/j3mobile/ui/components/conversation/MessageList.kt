package io.j3mobile.ui.components.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.j3mobile.protocol.OrchestrationCheckpointSummary
import io.j3mobile.protocol.OrchestrationMessage
import io.j3mobile.protocol.OrchestrationThreadActivity
import io.j3mobile.viewmodel.PendingApprovalInfo

/**
 * Unified item type for the conversation stream.
 * Sorting by [timestamp] interleaves messages and activities.
 */
sealed class ConversationItem(val timestamp: String, val sortKey: String) {
    class Msg(val msg: OrchestrationMessage) :
        ConversationItem(msg.createdAt, "msg-${msg.id.value}")

    class Act(val activity: OrchestrationThreadActivity) :
        ConversationItem(activity.createdAt, "act-${activity.id.value}")

    class Checkpoint(val checkpoint: OrchestrationCheckpointSummary) :
        ConversationItem(checkpoint.completedAt, "cp-${checkpoint.turnId.value}")

    class Approval(val info: PendingApprovalInfo) :
        ConversationItem("~", "approval")
}

@Composable
fun MessageList(
    messages: List<OrchestrationMessage>,
    activities: List<OrchestrationThreadActivity>,
    checkpoints: List<OrchestrationCheckpointSummary>,
    pendingApproval: PendingApprovalInfo?,
    onApprove: (PendingApprovalInfo) -> Unit,
    onReject: (PendingApprovalInfo) -> Unit,
    onRevertCheckpoint: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Build unified item list sorted by timestamp
    val items = buildList {
        messages.forEach { add(ConversationItem.Msg(it)) }
        activities.forEach { add(ConversationItem.Act(it)) }
        checkpoints.forEach { add(ConversationItem.Checkpoint(it)) }
        // Approval is always last (most recent)
        pendingApproval?.let { add(ConversationItem.Approval(it)) }
    }.sortedBy { it.timestamp }

    val listState = rememberLazyListState()

    // Auto-scroll to the newest item when the list changes
    LaunchedEffect(items.size) {
        if (items.isNotEmpty()) {
            listState.animateScrollToItem(items.lastIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = items,
            key = { it.sortKey },
        ) { item ->
            when (item) {
                is ConversationItem.Msg -> MessageBubble(item.msg)
                is ConversationItem.Act -> ActivityItem(item.activity)
                is ConversationItem.Checkpoint -> CheckpointMarker(
                    checkpoint = item.checkpoint,
                    onRevert = { onRevertCheckpoint(item.checkpoint.checkpointTurnCount) },
                )
                is ConversationItem.Approval -> ApprovalBanner(
                    info = item.info,
                    onApprove = { onApprove(item.info) },
                    onReject = { onReject(item.info) },
                )
            }
        }
    }
}
