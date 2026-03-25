package io.j3mobile.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Identifies an LLM provider backend. */
@Serializable
enum class ProviderKind {
    @SerialName("codex") CODEX,
    @SerialName("claudeAgent") CLAUDE_AGENT,
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
