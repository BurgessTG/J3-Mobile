package io.j3mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.j3mobile.domain.PendingApproval
import io.j3mobile.protocol.ModelSelection
import io.j3mobile.protocol.OrchestrationThread
import kotlinx.coroutines.launch

@Composable
fun ConversationScreen(
    thread: OrchestrationThread?,
    pendingApproval: PendingApproval?,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
    onSend: suspend (String, ModelSelection) -> Unit,
    onInterrupt: () -> Unit,
    onRespondToApproval: suspend (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf(TextFieldValue()) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onBack) { Text("Back") }
            OutlinedButton(onClick = onSettingsClick) { Text("Settings") }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text(thread?.title ?: "Conversation", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = thread?.session?.status ?: "No session",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (pendingApproval != null && thread?.id?.value == pendingApproval.threadId.value) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Approval pending", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { scope.launch { onRespondToApproval("approve") } }) {
                            Text("Approve")
                        }
                        OutlinedButton(onClick = { scope.launch { onRespondToApproval("deny") } }) {
                            Text("Deny")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(thread?.messages.orEmpty(), key = { it.id.value }) { messageItem ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = messageItem.role, style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = messageItem.text)
                    }
                }
            }
            items(thread?.activities.orEmpty(), key = { "activity-${it.id.value}" }) { activity ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = activity.kind, style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = activity.summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Message") },
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    val selection = thread?.modelSelection ?: return@Button
                    val text = message.text.trim()
                    if (text.isNotEmpty()) {
                        scope.launch { onSend(text, selection) }
                        message = TextFieldValue()
                    }
                },
            ) { Text("Send") }
            if (thread?.latestTurn?.state == "running") {
                OutlinedButton(onClick = onInterrupt) { Text("Interrupt") }
            }
        }
    }
}
