package io.j3mobile.domain

import io.j3mobile.network.BridgeWsClient
import io.j3mobile.protocol.ApprovalRequestId
import io.j3mobile.protocol.MessageId
import io.j3mobile.protocol.ModelSelection
import io.j3mobile.protocol.ProjectId
import io.j3mobile.protocol.ThreadId
import io.j3mobile.protocol.WsMethods
import io.j3mobile.protocol.WsResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement

class ThreadRepository(private val wsClient: BridgeWsClient) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun createProject(id: ProjectId, title: String, workspaceRoot: String): WsResponse {
        val command = buildJsonObject {
            put("type", JsonPrimitive("project.create"))
            put("commandId", JsonPrimitive(generateCommandId()))
            put("projectId", JsonPrimitive(id.value))
            put("title", JsonPrimitive(title))
            put("workspaceRoot", JsonPrimitive(workspaceRoot))
            put("createdAt", JsonPrimitive(""))
        }
        return dispatchCommand(command)
    }

    suspend fun createThread(
        id: ThreadId,
        projectId: ProjectId,
        title: String,
        modelSelection: ModelSelection,
        runtimeMode: String = "full-access",
    ): WsResponse {
        val command = buildJsonObject {
            put("type", JsonPrimitive("thread.create"))
            put("commandId", JsonPrimitive(generateCommandId()))
            put("threadId", JsonPrimitive(id.value))
            put("projectId", JsonPrimitive(projectId.value))
            put("title", JsonPrimitive(title))
            put("modelSelection", json.encodeToJsonElement(modelSelection))
            put("runtimeMode", JsonPrimitive(runtimeMode))
            put("interactionMode", JsonPrimitive("default"))
            put("createdAt", JsonPrimitive(""))
        }
        return dispatchCommand(command)
    }

    suspend fun sendMessage(
        threadId: ThreadId,
        text: String,
        modelSelection: ModelSelection,
        runtimeMode: String = "full-access",
    ): WsResponse {
        val messageId = MessageId(generateCommandId())
        val command = buildJsonObject {
            put("type", JsonPrimitive("thread.turn.start"))
            put("commandId", JsonPrimitive(generateCommandId()))
            put("threadId", JsonPrimitive(threadId.value))
            put("message", buildJsonObject {
                put("messageId", JsonPrimitive(messageId.value))
                put("role", JsonPrimitive("user"))
                put("text", JsonPrimitive(text))
                put("attachments", JsonArray(emptyList()))
            })
            put("modelSelection", json.encodeToJsonElement(modelSelection))
            put("runtimeMode", JsonPrimitive(runtimeMode))
            put("interactionMode", JsonPrimitive("default"))
            put("assistantDeliveryMode", JsonPrimitive("streaming"))
            put("createdAt", JsonPrimitive(""))
        }
        return dispatchCommand(command)
    }

    suspend fun interruptTurn(threadId: ThreadId): WsResponse {
        val command = buildJsonObject {
            put("type", JsonPrimitive("thread.turn.interrupt"))
            put("commandId", JsonPrimitive(generateCommandId()))
            put("threadId", JsonPrimitive(threadId.value))
            put("createdAt", JsonPrimitive(""))
        }
        return dispatchCommand(command)
    }

    suspend fun respondToApproval(
        threadId: ThreadId,
        requestId: ApprovalRequestId,
        decision: String,
    ): WsResponse {
        val command = buildJsonObject {
            put("type", JsonPrimitive("thread.approval.respond"))
            put("commandId", JsonPrimitive(generateCommandId()))
            put("threadId", JsonPrimitive(threadId.value))
            put("requestId", JsonPrimitive(requestId.value))
            put("decision", JsonPrimitive(decision))
            put("createdAt", JsonPrimitive(""))
        }
        return dispatchCommand(command)
    }

    suspend fun stopSession(threadId: ThreadId): WsResponse {
        val command = buildJsonObject {
            put("type", JsonPrimitive("thread.session.stop"))
            put("commandId", JsonPrimitive(generateCommandId()))
            put("threadId", JsonPrimitive(threadId.value))
            put("createdAt", JsonPrimitive(""))
        }
        return dispatchCommand(command)
    }

    suspend fun revertCheckpoint(threadId: ThreadId, turnCount: Int): WsResponse {
        val command = buildJsonObject {
            put("type", JsonPrimitive("thread.checkpoint.revert"))
            put("commandId", JsonPrimitive(generateCommandId()))
            put("threadId", JsonPrimitive(threadId.value))
            put("turnCount", JsonPrimitive(turnCount))
            put("createdAt", JsonPrimitive(""))
        }
        return dispatchCommand(command)
    }

    suspend fun deleteThread(threadId: ThreadId): WsResponse {
        val command = buildJsonObject {
            put("type", JsonPrimitive("thread.delete"))
            put("commandId", JsonPrimitive(generateCommandId()))
            put("threadId", JsonPrimitive(threadId.value))
            put("createdAt", JsonPrimitive(""))
        }
        return dispatchCommand(command)
    }

    private suspend fun dispatchCommand(command: JsonObject): WsResponse {
        val params = buildJsonObject {
            put("command", command)
        }
        return wsClient.sendRequest(WsMethods.ORCHESTRATION_DISPATCH_COMMAND, params)
    }

    private fun generateCommandId(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        val random = (1..12).map { chars.random() }.joinToString("")
        return "cmd-$random"
    }
}
