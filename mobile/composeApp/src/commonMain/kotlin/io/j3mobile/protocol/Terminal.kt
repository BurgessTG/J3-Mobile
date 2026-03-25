package io.j3mobile.protocol

import kotlinx.serialization.Serializable

/** Input for opening a new terminal session. */
@Serializable
data class TerminalOpenInput(
    val threadId: ThreadId,
    val terminalId: String,
    val cwd: String,
    val cols: Int? = null,
    val rows: Int? = null,
    val env: Map<String, String>? = null,
)

/** Input for writing data to a terminal. */
@Serializable
data class TerminalWriteInput(
    val threadId: ThreadId,
    val terminalId: String,
    val data: String,
)

/** Input for resizing a terminal. */
@Serializable
data class TerminalResizeInput(
    val threadId: ThreadId,
    val terminalId: String,
    val cols: Int,
    val rows: Int,
)

/** Input for clearing a terminal. */
@Serializable
data class TerminalClearInput(
    val threadId: ThreadId,
    val terminalId: String,
)

/** Input for closing a terminal. */
@Serializable
data class TerminalCloseInput(
    val threadId: ThreadId,
    val terminalId: String,
    val deleteHistory: Boolean? = null,
)

/** Input for restarting a terminal. */
@Serializable
data class TerminalRestartInput(
    val threadId: ThreadId,
    val terminalId: String,
    val cwd: String,
    val cols: Int,
    val rows: Int,
    val env: Map<String, String>? = null,
)

/** State of a terminal session. */
@Serializable
data class TerminalSessionSnapshot(
    val threadId: ThreadId,
    val terminalId: String,
    val cwd: String,
    val status: String,
    val pid: Int? = null,
    val history: String,
    val exitCode: Int? = null,
    val exitSignal: Int? = null,
    val updatedAt: String,
)

/** Push event for terminal state changes. */
@Serializable
data class TerminalEvent(
    val threadId: ThreadId,
    val terminalId: String,
    val type: String,
    val createdAt: String,
    val data: String? = null,
    val snapshot: TerminalSessionSnapshot? = null,
    val exitCode: Int? = null,
    val exitSignal: Int? = null,
    val message: String? = null,
    val hasRunningSubprocess: Boolean? = null,
)
