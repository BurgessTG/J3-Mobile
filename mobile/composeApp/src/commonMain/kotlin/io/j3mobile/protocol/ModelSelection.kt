package io.j3mobile.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Identifies an LLM provider backend. */
@Serializable(with = ProviderKindSerializer::class)
enum class ProviderKind {
    CODEX,
    CLAUDE_AGENT,
}

object ProviderKindSerializer : KSerializer<ProviderKind> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ProviderKind", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ProviderKind {
        return when (decoder.decodeString().trim()) {
            "claudeAgent", "claude_agent" -> ProviderKind.CLAUDE_AGENT
            else -> ProviderKind.CODEX
        }
    }

    override fun serialize(encoder: Encoder, value: ProviderKind) {
        val serialized = when (value) {
            ProviderKind.CODEX -> "codex"
            ProviderKind.CLAUDE_AGENT -> "claudeAgent"
        }
        encoder.encodeString(serialized)
    }
}

/** Specifies which provider and model to use for a turn. */
@Serializable
data class ModelSelection(
    val provider: ProviderKind,
    val model: String,
    val options: ModelOptions? = null,
)

/** Provider-specific tuning knobs. */
@Serializable
data class ModelOptions(
    // Codex options
    val reasoningEffort: String? = null,
    val fastMode: Boolean? = null,
    // Claude options
    val thinking: Boolean? = null,
    val effort: String? = null,
)
