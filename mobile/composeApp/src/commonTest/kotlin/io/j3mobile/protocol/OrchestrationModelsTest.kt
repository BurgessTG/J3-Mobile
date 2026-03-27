package io.j3mobile.protocol

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class OrchestrationModelsTest {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun decodesSnapshotWhenAssistantAttachmentsAreNull() {
        val payload =
            """
            {
              "snapshotSequence": 1,
              "projects": [
                {
                  "id": "project-1",
                  "title": "J3-Mobile",
                  "workspaceRoot": "/tmp/project",
                  "scripts": [],
                  "createdAt": "2026-03-25T00:00:00Z",
                  "updatedAt": "2026-03-25T00:00:00Z"
                }
              ],
              "threads": [
                {
                  "id": "thread-deleted",
                  "projectId": "project-1",
                  "title": "deleted thread",
                  "modelSelection": {
                    "provider": "",
                    "model": ""
                  },
                  "runtimeMode": "full-access",
                  "interactionMode": "chat",
                  "messages": [],
                  "proposedPlans": [],
                  "activities": [],
                  "checkpoints": [],
                  "createdAt": "2026-03-25T00:00:00Z",
                  "updatedAt": "2026-03-25T00:00:00Z",
                  "deletedAt": "2026-03-25T00:00:01Z"
                },
                {
                  "id": "thread-1",
                  "projectId": "project-1",
                  "title": "active thread",
                  "modelSelection": {
                    "provider": "",
                    "model": ""
                  },
                  "runtimeMode": "full-access",
                  "interactionMode": "chat",
                  "latestTurn": {
                    "turnId": "turn-1",
                    "state": "completed",
                    "requestedAt": "2026-03-25T00:00:00Z",
                    "assistantMessageId": "message-2"
                  },
                  "messages": [
                    {
                      "id": "message-1",
                      "role": "user",
                      "text": "hello",
                      "attachments": [],
                      "streaming": false,
                      "createdAt": "2026-03-25T00:00:00Z",
                      "updatedAt": "2026-03-25T00:00:00Z"
                    },
                    {
                      "id": "message-2",
                      "role": "assistant",
                      "text": "world",
                      "attachments": null,
                      "turnId": "turn-1",
                      "streaming": false,
                      "createdAt": "2026-03-25T00:00:01Z",
                      "updatedAt": "2026-03-25T00:00:01Z"
                    }
                  ],
                  "proposedPlans": [],
                  "activities": [],
                  "checkpoints": [],
                  "session": {
                    "threadId": "thread-1",
                    "status": "running",
                    "providerName": "codex",
                    "runtimeMode": "full-access",
                    "activeTurnId": "turn-1",
                    "updatedAt": "2026-03-25T00:00:01Z"
                  },
                  "createdAt": "2026-03-25T00:00:00Z",
                  "updatedAt": "2026-03-25T00:00:01Z"
                }
              ],
              "updatedAt": "2026-03-25T00:00:01Z"
            }
            """.trimIndent()

        val model = json.decodeFromString(OrchestrationReadModel.serializer(), payload)

        assertEquals(1, model.projects.size)
        assertEquals(2, model.threads.size)
        assertEquals(emptyList(), model.threads[1].messages[1].attachments)
        assertEquals(ProviderKind.CODEX, model.threads[1].modelSelection.provider)
    }
}
