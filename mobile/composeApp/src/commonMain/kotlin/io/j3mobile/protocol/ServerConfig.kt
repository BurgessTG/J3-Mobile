package io.j3mobile.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Server configuration snapshot. */
@Serializable
data class ServerConfig(
    val cwd: String,
    val keybindingsConfigPath: String,
    val keybindings: JsonElement? = null,
    val issues: List<ServerConfigIssue> = emptyList(),
    val providers: List<ServerProviderStatus> = emptyList(),
    val availableEditors: List<String> = emptyList(),
)

/** Describes the status of an LLM provider. */
@Serializable
data class ServerProviderStatus(
    val provider: ProviderKind,
    val status: String,
    val available: Boolean,
    val authStatus: String,
    val checkedAt: String,
    val message: String? = null,
)

/** Describes a configuration problem. */
@Serializable
data class ServerConfigIssue(
    val kind: String,
    val message: String,
    val index: Int? = null,
)
