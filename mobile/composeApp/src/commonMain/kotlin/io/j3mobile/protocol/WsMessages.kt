package io.j3mobile.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** Client-to-server request envelope. */
@Serializable
data class WsRequest(
    val id: String,
    val body: JsonObject,
)

/** Minimal shape inside body used for routing. */
@Serializable
data class WsRequestBody(
    @kotlinx.serialization.SerialName("_tag")
    val tag: String,
)

/** Server-to-client response envelope. */
@Serializable
data class WsResponse(
    val id: String,
    val result: JsonElement? = null,
    val error: WsError? = null,
)

/** Error payload inside a response. */
@Serializable
data class WsError(val message: String)

/** Server-to-client push envelope. */
@Serializable
data class WsPush(
    val type: String,
    val sequence: Int,
    val channel: String,
    val data: JsonElement,
)

/** WebSocket method names (client -> server). */
object WsMethods {
    const val ORCHESTRATION_GET_SNAPSHOT = "orchestration.getSnapshot"
    const val ORCHESTRATION_DISPATCH_COMMAND = "orchestration.dispatchCommand"
    const val ORCHESTRATION_GET_TURN_DIFF = "orchestration.getTurnDiff"
    const val ORCHESTRATION_GET_FULL_THREAD_DIFF = "orchestration.getFullThreadDiff"
    const val ORCHESTRATION_REPLAY_EVENTS = "orchestration.replayEvents"
    const val PROJECTS_SEARCH_ENTRIES = "projects.searchEntries"
    const val PROJECTS_WRITE_FILE = "projects.writeFile"
    const val GIT_PULL = "git.pull"
    const val GIT_STATUS = "git.status"
    const val GIT_RUN_STACKED_ACTION = "git.runStackedAction"
    const val GIT_LIST_BRANCHES = "git.listBranches"
    const val GIT_CREATE_WORKTREE = "git.createWorktree"
    const val GIT_REMOVE_WORKTREE = "git.removeWorktree"
    const val GIT_CREATE_BRANCH = "git.createBranch"
    const val GIT_CHECKOUT = "git.checkout"
    const val GIT_INIT = "git.init"
    const val GIT_RESOLVE_PULL_REQUEST = "git.resolvePullRequest"
    const val GIT_PREPARE_PULL_REQUEST_THREAD = "git.preparePullRequestThread"
    const val TERMINAL_OPEN = "terminal.open"
    const val TERMINAL_WRITE = "terminal.write"
    const val TERMINAL_RESIZE = "terminal.resize"
    const val TERMINAL_CLEAR = "terminal.clear"
    const val TERMINAL_RESTART = "terminal.restart"
    const val TERMINAL_CLOSE = "terminal.close"
    const val SHELL_OPEN_IN_EDITOR = "shell.openInEditor"
    const val SERVER_GET_CONFIG = "server.getConfig"
    const val SERVER_UPSERT_KEYBINDING = "server.upsertKeybinding"
}

/** WebSocket push channel names (server -> client). */
object WsChannels {
    const val SERVER_WELCOME = "server.welcome"
    const val SERVER_CONFIG_UPDATED = "server.configUpdated"
    const val ORCHESTRATION_DOMAIN_EVENT = "orchestration.domainEvent"
    const val TERMINAL_EVENT = "terminal.event"
    const val GIT_ACTION_PROGRESS = "git.actionProgress"
}
