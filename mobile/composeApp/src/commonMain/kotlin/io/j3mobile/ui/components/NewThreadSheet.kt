package io.j3mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.j3mobile.protocol.ModelSelection
import io.j3mobile.protocol.ProviderKind
import io.j3mobile.ui.theme.J3Secondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewThreadSheet(
    onDismiss: () -> Unit,
    onCreate: (title: String, modelSelection: ModelSelection) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf("") }
    var selectedProvider by remember { mutableStateOf(ProviderKind.CLAUDE_AGENT) }
    var modelName by remember { mutableStateOf("claude-sonnet-4-20250514") }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "New Conversation",
                style = MaterialTheme.typography.headlineMedium,
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                placeholder = { Text("What are you working on?") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            // Provider selection
            Text(
                text = "Provider",
                style = MaterialTheme.typography.labelLarge,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedProvider == ProviderKind.CLAUDE_AGENT,
                    onClick = {
                        selectedProvider = ProviderKind.CLAUDE_AGENT
                        modelName = "claude-sonnet-4-20250514"
                    },
                    label = { Text("Claude Agent") },
                )
                FilterChip(
                    selected = selectedProvider == ProviderKind.CODEX,
                    onClick = {
                        selectedProvider = ProviderKind.CODEX
                        modelName = "codex-mini"
                    },
                    label = { Text("Codex") },
                )
            }

            OutlinedTextField(
                value = modelName,
                onValueChange = { modelName = it },
                label = { Text("Model") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.labelSmall,
            )

            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onCreate(
                            title,
                            ModelSelection(
                                provider = selectedProvider,
                                model = modelName,
                            ),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = J3Secondary),
                enabled = title.isNotBlank(),
            ) {
                Text("Create")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
