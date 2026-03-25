package io.j3mobile.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Optional provider-level tracing information on events. */
@Serializable
data class EventMetadata(
    val providerTurnId: String? = null,
    val providerItemId: ProviderItemId? = null,
    val adapterKey: String? = null,
    val requestId: ApprovalRequestId? = null,
    val ingestedAt: String? = null,
)

/** A domain event with its payload kept as raw JSON. */
@Serializable
data class OrchestrationEvent(
    val sequence: Int,
    val eventId: EventId,
    val type: String,
    val aggregateKind: String,
    val aggregateId: String,
    val occurredAt: String,
    val commandId: CommandId? = null,
    val causationEventId: EventId? = null,
    val correlationId: CommandId? = null,
    val metadata: EventMetadata = EventMetadata(),
    val payload: JsonElement? = null,
)

/** Payload for thread message-sent events. */
@Serializable
data class ThreadMessageSentPayload(
    val threadId: ThreadId,
    val messageId: MessageId,
    val role: String,
    val text: String,
    val turnId: TurnId,
    val streaming: Boolean,
)

/** Payload for thread session-set events. */
@Serializable
data class ThreadSessionSetPayload(
    val threadId: ThreadId,
    val status: String,
    val providerName: String,
    val runtimeMode: String,
    val activeTurnId: TurnId,
    val lastError: String,
)

/** Payload for thread activity-appended events. */
@Serializable
data class ThreadActivityAppendedPayload(
    val threadId: ThreadId,
    val activityId: EventId,
    val tone: String,
    val kind: String,
    val summary: String,
    val payload: JsonElement? = null,
    val turnId: TurnId? = null,
)
