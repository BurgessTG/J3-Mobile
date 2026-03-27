package io.j3mobile.domain

import io.j3mobile.protocol.ApprovalRequestId
import io.j3mobile.protocol.ThreadId

data class PendingApproval(
    val threadId: ThreadId,
    val requestId: ApprovalRequestId,
    val sourceEventType: String,
)
