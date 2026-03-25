package io.j3mobile.protocol

import kotlinx.serialization.Serializable

/** Command type constants. */
object CommandTypes {
    const val PROJECT_CREATE = "project.create"
    const val PROJECT_META_UPDATE = "project.meta.update"
    const val PROJECT_DELETE = "project.delete"
    const val THREAD_CREATE = "thread.create"
    const val THREAD_DELETE = "thread.delete"
    const val THREAD_META_UPDATE = "thread.meta.update"
    const val THREAD_RUNTIME_MODE_SET = "thread.runtime-mode.set"
    const val THREAD_INTERACTION_MODE_SET = "thread.interaction-mode.set"
    const val THREAD_TURN_START = "thread.turn.start"
    const val THREAD_TURN_INTERRUPT = "thread.turn.interrupt"
    const val THREAD_APPROVAL_RESPOND = "thread.approval.respond"
    const val THREAD_USER_INPUT_RESPOND = "thread.user-input.respond"
    const val THREAD_CHECKPOINT_REVERT = "thread.checkpoint.revert"
    const val THREAD_SESSION_STOP = "thread.session.stop"
}

/** Creates a new project. */
@Serializable
data class ProjectCreateCommand(
    val type: String = CommandTypes.PROJECT_CREATE,
    val commandId: CommandId,
    val createdAt: String,
    val projectId: ProjectId,
    val title: String,
    val workspaceRoot: String,
    val defaultModelSelection: ModelSelection? = null,
)

/** Updates project metadata. */
@Serializable
data class ProjectMetaUpdateCommand(
    val type: String = CommandTypes.PROJECT_META_UPDATE,
    val commandId: CommandId,
    val createdAt: String,
    val projectId: ProjectId,
    val title: String? = null,
    val workspaceRoot: String? = null,
    val defaultModelSelection: ModelSelection? = null,
    val scripts: List<ProjectScript>? = null,
)

/** Deletes a project. */
@Serializable
data class ProjectDeleteCommand(
    val type: String = CommandTypes.PROJECT_DELETE,
    val commandId: CommandId,
    val createdAt: String,
    val projectId: ProjectId,
)

/** Creates a new thread. */
@Serializable
data class ThreadCreateCommand(
    val type: String = CommandTypes.THREAD_CREATE,
    val commandId: CommandId,
    val createdAt: String,
    val threadId: ThreadId,
    val projectId: ProjectId,
    val title: String,
    val modelSelection: ModelSelection,
    val runtimeMode: String,
    val interactionMode: String,
    val branch: String? = null,
    val worktreePath: String? = null,
)

/** Deletes a thread. */
@Serializable
data class ThreadDeleteCommand(
    val type: String = CommandTypes.THREAD_DELETE,
    val commandId: CommandId,
    val createdAt: String,
    val threadId: ThreadId,
)

/** Updates thread metadata. */
@Serializable
data class ThreadMetaUpdateCommand(
    val type: String = CommandTypes.THREAD_META_UPDATE,
    val commandId: CommandId,
    val createdAt: String,
    val threadId: ThreadId,
    val title: String? = null,
    val modelSelection: ModelSelection? = null,
    val branch: String? = null,
    val worktreePath: String? = null,
)

/** Changes the runtime mode for a thread. */
@Serializable
data class ThreadRuntimeModeSetCommand(
    val type: String = CommandTypes.THREAD_RUNTIME_MODE_SET,
    val commandId: CommandId,
    val createdAt: String,
    val threadId: ThreadId,
    val runtimeMode: String,
)

/** Changes the interaction mode for a thread. */
@Serializable
data class ThreadInteractionModeSetCommand(
    val type: String = CommandTypes.THREAD_INTERACTION_MODE_SET,
    val commandId: CommandId,
    val createdAt: String,
    val threadId: ThreadId,
    val interactionMode: String,
)

/** The user message that initiates a turn. */
@Serializable
data class TurnStartMessage(
    val messageId: MessageId,
    val role: String,
    val text: String,
    val attachments: List<UploadChatAttachment> = emptyList(),
)

/** An attachment being sent with a turn-start message. */
@Serializable
data class UploadChatAttachment(
    val type: String,
    val id: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Int,
    val dataUrl: String,
)

/** Starts a new turn in a thread. */
@Serializable
data class ThreadTurnStartCommand(
    val type: String = CommandTypes.THREAD_TURN_START,
    val commandId: CommandId,
    val createdAt: String,
    val threadId: ThreadId,
    val message: TurnStartMessage,
    val modelSelection: ModelSelection? = null,
    val runtimeMode: String,
    val interactionMode: String,
    val assistantDeliveryMode: String,
)

/** Interrupts the current turn. */
@Serializable
data class ThreadTurnInterruptCommand(
    val type: String = CommandTypes.THREAD_TURN_INTERRUPT,
    val commandId: CommandId,
    val createdAt: String,
    val threadId: ThreadId,
)

/** Responds to an approval request. */
@Serializable
data class ThreadApprovalRespondCommand(
    val type: String = CommandTypes.THREAD_APPROVAL_RESPOND,
    val commandId: CommandId,
    val createdAt: String,
    val threadId: ThreadId,
    val requestId: ApprovalRequestId,
    val decision: String,
)

/** Responds to a user-input request. */
@Serializable
data class ThreadUserInputRespondCommand(
    val type: String = CommandTypes.THREAD_USER_INPUT_RESPOND,
    val commandId: CommandId,
    val createdAt: String,
    val threadId: ThreadId,
    val requestId: ApprovalRequestId,
    val answers: Map<String, String>,
)

/** Reverts to a checkpoint. */
@Serializable
data class ThreadCheckpointRevertCommand(
    val type: String = CommandTypes.THREAD_CHECKPOINT_REVERT,
    val commandId: CommandId,
    val createdAt: String,
    val threadId: ThreadId,
    val turnCount: Int,
)

/** Stops the runtime session for a thread. */
@Serializable
data class ThreadSessionStopCommand(
    val type: String = CommandTypes.THREAD_SESSION_STOP,
    val commandId: CommandId,
    val createdAt: String,
    val threadId: ThreadId,
)
