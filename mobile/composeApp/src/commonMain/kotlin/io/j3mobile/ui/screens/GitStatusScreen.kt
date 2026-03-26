package io.j3mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.j3mobile.ui.theme.J3Border
import io.j3mobile.ui.theme.J3Error
import io.j3mobile.ui.theme.J3OnDarkSecondary
import io.j3mobile.ui.theme.J3Primary
import io.j3mobile.ui.theme.J3Secondary
import io.j3mobile.viewmodel.GitStatusViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitStatusSheet(
    gitVm: GitStatusViewModel,
    cwd: String,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val status by gitVm.status.collectAsState()
    val branches by gitVm.branches.collectAsState()

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Git Status",
                style = MaterialTheme.typography.headlineMedium,
            )

            status?.let { s ->
                // Branch name
                s.branch?.let { branch ->
                    Text(
                        text = "Branch: $branch",
                        style = MaterialTheme.typography.titleMedium,
                        color = J3Primary,
                    )
                }

                // Ahead/Behind
                if (s.aheadCount > 0 || s.behindCount > 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (s.aheadCount > 0) {
                            Text(
                                text = "${s.aheadCount} ahead",
                                style = MaterialTheme.typography.bodySmall,
                                color = J3Secondary,
                            )
                        }
                        if (s.behindCount > 0) {
                            Text(
                                text = "${s.behindCount} behind",
                                style = MaterialTheme.typography.bodySmall,
                                color = J3Error,
                            )
                        }
                    }
                }

                HorizontalDivider(color = J3Border)

                // File changes
                if (s.workingTree.files.isNotEmpty()) {
                    Text(
                        text = "Changes (${s.workingTree.files.size} files)",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    s.workingTree.files.forEach { file ->
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
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (file.insertions > 0) {
                                    Text(
                                        "+${file.insertions}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = J3Secondary,
                                    )
                                }
                                if (file.deletions > 0) {
                                    Text(
                                        "-${file.deletions}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = J3Error,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "No changes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = J3OnDarkSecondary,
                    )
                }

                // Pull button
                Button(
                    onClick = { gitVm.pull(cwd) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Pull")
                }
            } ?: Text(
                text = "Loading...",
                style = MaterialTheme.typography.bodyMedium,
                color = J3OnDarkSecondary,
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}
