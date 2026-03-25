package io.j3mobile.ui

import androidx.compose.runtime.*
import io.j3mobile.protocol.ProjectId
import io.j3mobile.protocol.ThreadId
import io.j3mobile.ui.navigation.Screen
import io.j3mobile.ui.screens.*
import io.j3mobile.ui.theme.J3MobileTheme

@Composable
fun J3MobileApp() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Projects) }

    J3MobileTheme {
        when (val screen = currentScreen) {
            is Screen.Projects -> ProjectsScreen(
                onProjectSelected = { id ->
                    currentScreen = Screen.Threads(ProjectId(id))
                },
                onSettingsClick = {
                    currentScreen = Screen.Settings
                },
            )
            is Screen.Threads -> ProjectsScreen() // Placeholder
            is Screen.Conversation -> ConversationScreen(
                threadId = screen.threadId.value,
                onBack = { currentScreen = Screen.Projects },
            )
            is Screen.Terminal -> ConversationScreen() // Placeholder
            is Screen.Settings -> SettingsScreen(
                onBack = { currentScreen = Screen.Projects },
            )
        }
    }
}
