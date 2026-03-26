package io.j3mobile.ui.navigation

import io.j3mobile.protocol.ProjectId
import io.j3mobile.protocol.ThreadId

sealed class Screen {
    data object Projects : Screen()
    data class Threads(val projectId: ProjectId) : Screen()
    data class Conversation(val threadId: ThreadId) : Screen()
    data object Settings : Screen()
}
