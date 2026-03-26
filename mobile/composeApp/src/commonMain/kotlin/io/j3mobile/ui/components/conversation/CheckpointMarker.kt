package io.j3mobile.ui.components.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.j3mobile.protocol.OrchestrationCheckpointSummary
import io.j3mobile.ui.theme.J3Border
import io.j3mobile.ui.theme.J3OnDarkSecondary
import io.j3mobile.ui.theme.J3Secondary
import io.j3mobile.ui.theme.J3Warning

@Composable
fun CheckpointMarker(
    checkpoint: OrchestrationCheckpointSummary,
    onRevert: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    val totalAdditions = checkpoint.files.sumOf { it.additions }
    val totalDeletions = checkpoint.files.sumOf { it.deletions }
    val fileCount = checkpoint.files.size

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Divider with centered label
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = J3Border,
            )
            Text(
                text = " Checkpoint ${checkpoint.checkpointTurnCount} ",
                style = MaterialTheme.typography.labelSmall,
                color = J3OnDarkSecondary,
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = J3Border,
            )
        }

        // File stats
        if (fileCount > 0) {
            Text(
                text = "+$totalAdditions -$totalDeletions across $fileCount file${if (fileCount != 1) "s" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = J3OnDarkSecondary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        // Revert button
        TextButton(onClick = onRevert) {
            Text(
                text = "Revert",
                color = J3Warning,
                style = MaterialTheme.typography.labelLarge,
            )
        }

        // Expanded file list
        if (expanded && checkpoint.files.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                checkpoint.files.forEach { file ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = file.path,
                            style = MaterialTheme.typography.labelSmall,
                            color = J3OnDarkSecondary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "+${file.additions} -${file.deletions}",
                            style = MaterialTheme.typography.labelSmall,
                            color = J3Secondary,
                        )
                    }
                }
            }
        }
    }
}
