package io.j3mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.j3mobile.network.ConnectionState
import io.j3mobile.protocol.ModelSelection
import io.j3mobile.protocol.OrchestrationProject
import io.j3mobile.protocol.OrchestrationThread
import io.j3mobile.protocol.ProviderKind
import io.j3mobile.protocol.ThreadId
import io.j3mobile.ui.theme.J3Border
import io.j3mobile.ui.theme.J3DarkCard
import io.j3mobile.ui.util.formatRelativeTime
import kotlinx.coroutines.launch

@Composable
fun ProjectsScreen(
    connectionState: ConnectionState,
    projects: List<OrchestrationProject>,
    threads: List<OrchestrationThread>,
    onThreadSelected: (String) -> Unit,
    onCreateProject: suspend (String, String) -> Unit,
    onCreateThread: suspend (String, String, ModelSelection, String) -> ThreadId,
    onSettingsClick: () -> Unit,
    onRetry: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var newestFirst by rememberSaveable { mutableStateOf(true) }
    var showCreateProjectDialog by rememberSaveable { mutableStateOf(false) }
    var projectTitle by rememberSaveable { mutableStateOf("") }
    var workspaceRoot by rememberSaveable { mutableStateOf("") }

    val threadsByProject = remember(projects, threads, newestFirst) {
        projects.associate { project ->
            val projectThreads = threads
                .filter { it.projectId == project.id }
                .sortedBy { it.updatedAt }
                .let { if (newestFirst) it.reversed() else it }
            project.id.value to projectThreads
        }
    }
    val sortedProjects = remember(projects, threadsByProject, newestFirst) {
        projects.sortedBy { project ->
            threadsByProject[project.id.value]?.firstOrNull()?.updatedAt ?: project.updatedAt
        }.let { if (newestFirst) it.reversed() else it }
    }

    LaunchedEffect(showCreateProjectDialog) {
        if (!showCreateProjectDialog) {
            projectTitle = ""
            workspaceRoot = ""
        }
    }

    if (showCreateProjectDialog) {
        AlertDialog(
            onDismissRequest = { showCreateProjectDialog = false },
            title = { Text("Add Project") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = projectTitle,
                        onValueChange = { projectTitle = it },
                        label = { Text("Title") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = workspaceRoot,
                        onValueChange = { workspaceRoot = it },
                        label = { Text("Workspace path") },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            onCreateProject(projectTitle.trim(), workspaceRoot.trim())
                        }
                        showCreateProjectDialog = false
                    },
                    enabled = projectTitle.isNotBlank() && workspaceRoot.isNotBlank(),
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateProjectDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "PROJECTS",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { newestFirst = !newestFirst }) {
                        Icon(
                            imageVector = Icons.Outlined.SwapVert,
                            contentDescription = "Toggle project sort",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { showCreateProjectDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "Add project",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = when (connectionState) {
                        ConnectionState.Disconnected -> "Disconnected"
                        ConnectionState.Connecting -> "Connecting"
                        is ConnectionState.Connected -> "Connected"
                        is ConnectionState.Reconnecting -> "Reconnecting"
                        is ConnectionState.Error -> "Connection error"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (connectionState !is ConnectionState.Connected) {
                        TextButton(onClick = onRetry) {
                            Text("Retry")
                        }
                    }
                    TextButton(onClick = onSettingsClick) {
                        Text("Bridge")
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (sortedProjects.isEmpty()) {
                Text(
                    text = "No projects yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(sortedProjects, key = { it.id.value }) { project ->
                        val projectThreads = threadsByProject[project.id.value].orEmpty()
                        ProjectSection(
                            project = project,
                            threads = projectThreads,
                            onThreadSelected = onThreadSelected,
                            onCreateThread = {
                                val inferredSelection = inferThreadModel(project, projectThreads)
                                val inferredRuntimeMode = projectThreads.firstOrNull()?.runtimeMode ?: "full-access"
                                scope.launch {
                                    val threadId = onCreateThread(
                                        project.id.value,
                                        "New thread",
                                        inferredSelection,
                                        inferredRuntimeMode,
                                    )
                                    onThreadSelected(threadId.value)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectSection(
    project: OrchestrationProject,
    threads: List<OrchestrationThread>,
    onThreadSelected: (String) -> Unit,
    onCreateThread: () -> Unit,
) {
    var expanded by rememberSaveable(project.id.value) { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = if (expanded) Icons.Outlined.KeyboardArrowDown else Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                imageVector = Icons.Outlined.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = project.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TextButton(onClick = onCreateThread) {
                Text("New")
            }
        }

        if (expanded) {
            if (threads.isEmpty()) {
                Text(
                    text = "No threads yet.",
                    modifier = Modifier.padding(start = 42.dp, top = 6.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 11.dp, top = 6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height((threads.size * 56).dp.coerceAtLeast(36.dp))
                            .background(J3Border),
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        threads.forEach { thread ->
                            ThreadRow(
                                thread = thread,
                                onClick = { onThreadSelected(thread.id.value) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreadRow(
    thread: OrchestrationThread,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        color = J3DarkCard,
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = thread.title.ifBlank { "New thread" },
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
            )
            val relativeTime = formatRelativeTime(thread.updatedAt)
            if (relativeTime.isNotBlank()) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = relativeTime,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private fun inferThreadModel(
    project: OrchestrationProject,
    threads: List<OrchestrationThread>,
): ModelSelection {
    return project.defaultModelSelection
        ?: threads.firstOrNull { it.modelSelection.model.isNotBlank() }?.modelSelection
        ?: threads.firstOrNull()?.modelSelection
        ?: ModelSelection(
            provider = ProviderKind.CODEX,
            model = "",
        )
}
