package io.j3mobile.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

/** Top-level snapshot returned by getSnapshot. */
@Serializable
data class OrchestrationReadModel(
    val snapshotSequence: Int,
    val projects: List<OrchestrationProject>,
    val threads: List<OrchestrationThread>,
    val updatedAt: String,
)

/** A workspace project. */
@Serializable
data class OrchestrationProject(
    val id: ProjectId,
    val title: String,
    val workspaceRoot: String,
    val defaultModelSelection: ModelSelection? = null,
    val scripts: List<ProjectScript> = emptyList(),
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
)

/** A user-defined script attached to a project. */
@Serializable
data class ProjectScript(
    val id: String,
    val name: String,
    val command: String,
    val icon: String,
    val runOnWorktreeCreate: Boolean,
)

/** A conversation thread. */
@Serializable
data class OrchestrationThread(
    val id: ThreadId,
    val projectId: ProjectId,
    val title: String,
    val modelSelection: ModelSelection,
    val runtimeMode: String,
    val interactionMode: String,
    val branch: String? = null,
    val worktreePath: String? = null,
    val latestTurn: OrchestrationLatestTurn? = null,
    val messages: List<OrchestrationMessage> = emptyList(),
    val proposedPlans: List<OrchestrationProposedPlan> = emptyList(),
    val activities: List<OrchestrationThreadActivity> = emptyList(),
    val checkpoints: List<OrchestrationCheckpointSummary> = emptyList(),
    val session: OrchestrationSession? = null,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
)

/** A single message in a thread. */
@Serializable
data class OrchestrationMessage(
    val id: MessageId,
    val role: String,
    val text: String,
    @Serializable(with = ChatAttachmentListSerializer::class)
    val attachments: List<ChatAttachment> = emptyList(),
    val turnId: TurnId? = null,
    val streaming: Boolean,
    val createdAt: String,
    val updatedAt: String,
)

object ChatAttachmentListSerializer : KSerializer<List<ChatAttachment>> {
    private val delegate = ListSerializer(ChatAttachment.serializer())

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): List<ChatAttachment> {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeSerializableValue(delegate)
        val element = jsonDecoder.decodeJsonElement()
        if (element is JsonNull) {
            return emptyList()
        }
        return jsonDecoder.json.decodeFromJsonElement(delegate, element)
    }

    override fun serialize(encoder: Encoder, value: List<ChatAttachment>) {
        encoder.encodeSerializableValue(delegate, value)
    }
}

/** A file attachment on a message. */
@Serializable
data class ChatAttachment(
    val type: String,
    val id: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Int,
)

/** Tracks the most recent turn in a thread. */
@Serializable
data class OrchestrationLatestTurn(
    val turnId: TurnId,
    val state: String,
    val requestedAt: String,
    val startedAt: String? = null,
    val completedAt: String? = null,
    val assistantMessageId: MessageId? = null,
)

/** Describes the runtime session for a thread. */
@Serializable
data class OrchestrationSession(
    val threadId: ThreadId,
    val status: String,
    val providerName: String? = null,
    val runtimeMode: String,
    val activeTurnId: TurnId? = null,
    val lastError: String? = null,
    val updatedAt: String,
)

/** A plan the assistant has proposed. */
@Serializable
data class OrchestrationProposedPlan(
    val id: String,
    val turnId: TurnId? = null,
    val planMarkdown: String,
    val implementedAt: String? = null,
    val implementationThreadId: ThreadId? = null,
    val createdAt: String,
    val updatedAt: String,
)

/** Describes a checkpoint in a thread. */
@Serializable
data class OrchestrationCheckpointSummary(
    val turnId: TurnId,
    val checkpointTurnCount: Int,
    val checkpointRef: CheckpointRef,
    val status: String,
    val files: List<OrchestrationCheckpointFile> = emptyList(),
    val assistantMessageId: MessageId? = null,
    val completedAt: String,
)

/** Describes a file changed in a checkpoint. */
@Serializable
data class OrchestrationCheckpointFile(
    val path: String,
    val kind: String,
    val additions: Int,
    val deletions: Int,
)

/** An activity event in a thread. */
@Serializable
data class OrchestrationThreadActivity(
    val id: EventId,
    val tone: String,
    val kind: String,
    val summary: String,
    val payload: JsonElement? = null,
    val turnId: TurnId? = null,
    val sequence: Int? = null,
    val createdAt: String,
)
