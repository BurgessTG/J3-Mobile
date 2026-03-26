package io.j3mobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import io.j3mobile.domain.AppContainer
import io.j3mobile.protocol.ProjectId
import io.j3mobile.protocol.ThreadId
import io.j3mobile.ui.navigation.Screen
import io.j3mobile.ui.screens.*
import io.j3mobile.ui.theme.J3MobileTheme
import kotlinx.coroutines.launch

@Composable
fun J3MobileApp() {
    val container = remember { AppContainer() }
    J3MobileApp(container)
}

@Composable
private fun J3MobileApp(container: AppContainer) {
    val scope = rememberCoroutineScope()
    val bridgeSettings by container.bridgeSettings.collectAsState()
    val connectionState by container.connectionState.collectAsState()
    val snapshot by container.store.readModel.collectAsState()
    val pendingApproval by container.pendingApproval.collectAsState()
    val navStack = remember { mutableStateListOf<Screen>(Screen.Projects) }

    LaunchedEffect(Unit) {
        container.bootstrap()
    }

    LaunchedEffect(bridgeSettings, navStack.lastOrNull()) {
        if (bridgeSettings == null && navStack.lastOrNull() == Screen.Projects) {
            navStack.clear()
            navStack.add(Screen.Settings)
        } else if (bridgeSettings != null && navStack.size == 1 && navStack.lastOrNull() == Screen.Settings) {
            navStack.clear()
            navStack.add(Screen.Projects)
        }
    }

    J3MobileTheme {
        when (val screen = navStack.lastOrNull() ?: Screen.Projects) {
            is Screen.Settings -> SettingsScreen(
                currentSettings = bridgeSettings,
                onSave = { baseUrl, jwt ->
                    container.saveBridgeSettings(baseUrl, jwt)
                    if (navStack.lastOrNull() == Screen.Settings) {
                        navStack.removeAt(navStack.lastIndex)
                    }
                },
                onClear = {
                    container.clearBridgeSettings()
                    navStack.clear()
                    navStack.add(Screen.Settings)
                },
                onBack = {
                    if (navStack.size > 1) navStack.removeAt(navStack.lastIndex)
                },
            )
            is Screen.Threads -> {
                val project = snapshot?.projects.orEmpty().find { it.id == screen.projectId }
                ThreadsScreen(
                    project = project,
                    threads = snapshot?.threads.orEmpty().filter { it.projectId == screen.projectId },
                    connectionState = connectionState,
                    onBack = {
                        if (navStack.size > 1) navStack.removeAt(navStack.lastIndex)
                    },
                    onSettingsClick = { navStack.add(Screen.Settings) },
                    onThreadSelected = { threadId -> navStack.add(Screen.Conversation(ThreadId(threadId))) },
                    onCreateThread = { title, modelSelection, runtimeMode ->
                        container.createThread(
                            projectId = screen.projectId,
                            title = title,
                            modelSelection = modelSelection,
                            runtimeMode = runtimeMode,
                        )
                    },
                )
            }
            is Screen.Conversation -> {
                val thread = snapshot?.threads.orEmpty().find { it.id == screen.threadId }
                ConversationScreen(
                    thread = thread,
                    pendingApproval = pendingApproval,
                    onBack = {
                        if (navStack.size > 1) navStack.removeAt(navStack.lastIndex)
                    },
                    onSettingsClick = { navStack.add(Screen.Settings) },
                    onSend = { text, selection ->
                        scope.launch {
                            container.sendMessage(
                                threadId = screen.threadId,
                                text = text,
                                modelSelection = selection,
                            )
                        }
                    },
                    onInterrupt = {
                        scope.launch { container.interruptTurn(screen.threadId) }
                    },
                    onRespondToApproval = { decision ->
                        val approval = pendingApproval
                        if (approval?.threadId == screen.threadId) {
                            scope.launch {
                                container.respondToApproval(
                                    threadId = screen.threadId,
                                    requestId = approval.requestId,
                                    decision = decision,
                                )
                            }
                        }
                    },
                )
            }
            Screen.Projects -> ProjectsScreen(
                connectionState = connectionState,
                projects = snapshot?.projects.orEmpty(),
                threads = snapshot?.threads.orEmpty(),
                onThreadSelected = { threadId -> navStack.add(Screen.Conversation(ThreadId(threadId))) },
                onCreateProject = { title, workspaceRoot ->
                    scope.launch {
                        container.createProject(
                            title = title,
                            workspaceRoot = workspaceRoot,
                        )
                    }
                },
                onCreateThread = { projectId, title, modelSelection, runtimeMode ->
                    container.createThread(
                        projectId = ProjectId(projectId),
                        title = title,
                        modelSelection = modelSelection,
                        runtimeMode = runtimeMode,
                    )
                },
                onSettingsClick = { navStack.add(Screen.Settings) },
                onRetry = { container.retryConnection() },
            )
        }
    }
}
