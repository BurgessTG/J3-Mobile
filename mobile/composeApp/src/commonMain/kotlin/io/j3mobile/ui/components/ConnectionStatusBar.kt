package io.j3mobile.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.j3mobile.network.ConnectionState
import io.j3mobile.ui.theme.J3Error
import io.j3mobile.ui.theme.J3OnDark
import io.j3mobile.ui.theme.J3Warning

@Composable
fun ConnectionStatusBar(
    connectionState: ConnectionState,
    onRetry: () -> Unit = {},
    onSettings: () -> Unit = {},
) {
    val visible = connectionState !is ConnectionState.Connected

    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(),
        exit = shrinkVertically(),
    ) {
        val bgColor = when (connectionState) {
            is ConnectionState.Reconnecting -> J3Warning
            is ConnectionState.Error -> J3Error
            is ConnectionState.Disconnected -> J3Error.copy(alpha = 0.85f)
            else -> J3Warning
        }

        val message = when (connectionState) {
            is ConnectionState.Disconnected -> "Not connected"
            is ConnectionState.Connecting -> "Connecting..."
            is ConnectionState.Reconnecting -> "Reconnecting... (attempt ${connectionState.attempt + 1})"
            is ConnectionState.Error -> connectionState.message
            is ConnectionState.Connected -> ""
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = J3OnDark,
                modifier = Modifier.weight(1f),
            )
            when (connectionState) {
                is ConnectionState.Reconnecting -> {
                    TextButton(onClick = onRetry) {
                        Text("Retry", color = J3OnDark)
                    }
                }
                is ConnectionState.Disconnected, is ConnectionState.Error -> {
                    TextButton(onClick = onSettings) {
                        Text("Settings", color = J3OnDark)
                    }
                }
                else -> {}
            }
        }
    }
}
