package io.j3mobile.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.j3mobile.di.appModule
import io.j3mobile.protocol.ProjectId
import io.j3mobile.protocol.ThreadId
import io.j3mobile.ui.navigation.Screen
import io.j3mobile.ui.screens.ConversationScreen
import io.j3mobile.ui.screens.ProjectsScreen
import io.j3mobile.ui.screens.SettingsScreen
import io.j3mobile.ui.screens.TerminalScreen
import io.j3mobile.ui.screens.ThreadListScreen
import io.j3mobile.ui.theme.J3MobileTheme
import org.koin.compose.KoinApplication

@Composable
fun J3MobileApp() {
    KoinApplication(application = { modules(appModule) }) {
        J3MobileTheme {
            var currentScreen by remember { mutableStateOf<Screen>(Screen.Projects) }
            var lastProjectId by remember { mutableStateOf<ProjectId?>(null) }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                when (val screen = currentScreen) {
                    is Screen.Projects -> ProjectsScreen(
                        onProjectSelected = { id ->
                            val pid = ProjectId(id)
                            lastProjectId = pid
                            currentScreen = Screen.Threads(pid)
                        },
                        onSettingsClick = {
                            currentScreen = Screen.Settings
                        },
                    )

                    is Screen.Threads -> ThreadListScreen(
                        projectId = screen.projectId,
                        onThreadSelected = { threadId ->
                            currentScreen = Screen.Conversation(threadId)
                        },
                        onBack = {
                            currentScreen = Screen.Projects
                        },
                    )

                    is Screen.Conversation -> ConversationScreen(
                        threadId = screen.threadId,
                        onBack = {
                            currentScreen = lastProjectId?.let { Screen.Threads(it) }
                                ?: Screen.Projects
                        },
                        onTerminal = {
                            currentScreen = Screen.Terminal(screen.threadId)
                        },
                    )

                    is Screen.Terminal -> TerminalScreen(
                        threadId = screen.threadId,
                        onBack = {
                            currentScreen = Screen.Conversation(screen.threadId)
                        },
                    )

                    is Screen.Settings -> SettingsScreen(
                        onBack = {
                            currentScreen = Screen.Projects
                        },
                    )
                }
            }
        }
    }
}
