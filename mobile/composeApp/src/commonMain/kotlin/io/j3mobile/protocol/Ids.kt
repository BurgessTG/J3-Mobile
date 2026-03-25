package io.j3mobile.protocol

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable @JvmInline value class ThreadId(val value: String)
@Serializable @JvmInline value class ProjectId(val value: String)
@Serializable @JvmInline value class CommandId(val value: String)
@Serializable @JvmInline value class EventId(val value: String)
@Serializable @JvmInline value class MessageId(val value: String)
@Serializable @JvmInline value class TurnId(val value: String)
@Serializable @JvmInline value class CheckpointRef(val value: String)
@Serializable @JvmInline value class ApprovalRequestId(val value: String)
@Serializable @JvmInline value class ProviderItemId(val value: String)
@Serializable @JvmInline value class RuntimeSessionId(val value: String)
