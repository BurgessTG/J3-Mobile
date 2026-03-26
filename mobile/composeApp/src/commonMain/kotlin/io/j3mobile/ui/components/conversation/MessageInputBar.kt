package io.j3mobile.ui.components.conversation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.j3mobile.ui.theme.J3DarkSurface
import io.j3mobile.ui.theme.J3Error
import io.j3mobile.ui.theme.J3OnDarkSecondary
import io.j3mobile.ui.theme.J3Primary

@Composable
fun MessageInputBar(
    isRunning: Boolean,
    onSend: (String) -> Unit,
    onInterrupt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf("") }

    Surface(
        color = J3DarkSurface,
        tonalElevation = 3.dp,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = {
                    Text(
                        "Message...",
                        color = J3OnDarkSecondary,
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                maxLines = 5,
                textStyle = MaterialTheme.typography.bodyMedium,
            )

            if (isRunning) {
                // Stop / interrupt button
                IconButton(
                    onClick = onInterrupt,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = J3Error,
                    ),
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(48.dp),
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Stop",
                    )
                }
            } else {
                // Send button
                IconButton(
                    onClick = {
                        if (text.isNotBlank()) {
                            onSend(text.trim())
                            text = ""
                        }
                    },
                    enabled = text.isNotBlank(),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = J3Primary,
                        disabledContentColor = J3OnDarkSecondary.copy(alpha = 0.4f),
                    ),
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(48.dp),
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                    )
                }
            }
        }
    }
}
