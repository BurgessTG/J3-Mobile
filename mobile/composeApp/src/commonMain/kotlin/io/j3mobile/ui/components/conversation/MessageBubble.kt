package io.j3mobile.ui.components.conversation

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import io.j3mobile.protocol.OrchestrationMessage
import io.j3mobile.ui.theme.J3Dark
import io.j3mobile.ui.theme.J3DarkCard
import io.j3mobile.ui.theme.J3OnDark
import io.j3mobile.ui.theme.J3OnDarkSecondary
import io.j3mobile.ui.theme.J3Primary

@Composable
fun MessageBubble(message: OrchestrationMessage) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        val bubbleShape = if (isUser) {
            RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
        } else {
            RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
        }

        Surface(
            shape = bubbleShape,
            color = if (isUser) J3Primary else J3DarkCard,
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUser) J3Dark else J3OnDark,
                    )

                    // Blinking cursor for streaming messages
                    if (message.streaming && !isUser) {
                        val transition = rememberInfiniteTransition()
                        val cursorAlpha by transition.animateFloat(
                            initialValue = 1f,
                            targetValue = 0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(500),
                                repeatMode = RepeatMode.Reverse,
                            ),
                        )
                        Text(
                            text = "|",
                            style = MaterialTheme.typography.bodyMedium,
                            color = J3OnDark,
                            modifier = Modifier.alpha(cursorAlpha),
                        )
                    }
                }

                // Timestamp
                Text(
                    text = formatTimestamp(message.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isUser) {
                        J3Dark.copy(alpha = 0.6f)
                    } else {
                        J3OnDarkSecondary
                    },
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** Extract time portion from ISO 8601 or show raw if unparseable. */
private fun formatTimestamp(iso: String): String {
    // Simple extraction: take HH:MM from "...THH:MM:SS..."
    val tIndex = iso.indexOf('T')
    if (tIndex >= 0 && iso.length >= tIndex + 6) {
        return iso.substring(tIndex + 1, tIndex + 6)
    }
    return ""
}
