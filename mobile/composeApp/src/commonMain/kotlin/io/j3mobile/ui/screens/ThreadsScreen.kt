package io.j3mobile.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.j3mobile.network.ConnectionState
import io.j3mobile.protocol.ModelSelection
import io.j3mobile.protocol.OrchestrationProject
import io.j3mobile.protocol.OrchestrationThread
import io.j3mobile.protocol.ProviderKind
import io.j3mobile.protocol.ThreadId
import kotlinx.coroutines.launch

@Composable
fun ThreadsScreen(
    project: OrchestrationProject?,
    threads: List<OrchestrationThread>,
    connectionState: ConnectionState,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
    onThreadSelected: (String) -> Unit,
    onCreateThread: suspend (String, ModelSelection, String) -> ThreadId,
) {
    val scope = rememberCoroutineScope()
    val inferredSelection = project?.defaultModelSelection
        ?: threads.firstOrNull { it.modelSelection.model.isNotBlank() }?.modelSelection
        ?: threads.firstOrNull()?.modelSelection
        ?: ModelSelection(
            provider = ProviderKind.CODEX,
            model = "",
        )
    val inferredRuntimeMode = threads.firstOrNull()?.runtimeMode ?: "full-access"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack) { Text("Back") }
                OutlinedButton(onClick = onSettingsClick) { Text("Settings") }
            }
            TextButton(
                onClick = {
                    scope.launch {
                        val threadId = onCreateThread(
                            "New thread",
                            inferredSelection,
                            inferredRuntimeMode,
                        )
                        onThreadSelected(threadId.value)
                    }
                },
            ) {
                Text("New Thread")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(project?.title ?: "Threads", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = when (connectionState) {
                ConnectionState.Disconnected -> "Disconnected"
                ConnectionState.Connecting -> "Connecting"
                is ConnectionState.Connected -> "Connected"
                is ConnectionState.Reconnecting -> "Reconnecting"
                is ConnectionState.Error -> "Connection error"
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (threads.isEmpty()) {
            Text(
                text = "No threads for this project yet.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(threads, key = { it.id.value }) { thread ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThreadSelected(thread.id.value) },
                        colors = CardDefaults.cardColors(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = thread.title, style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = thread.branch ?: thread.worktreePath ?: thread.id.value,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
