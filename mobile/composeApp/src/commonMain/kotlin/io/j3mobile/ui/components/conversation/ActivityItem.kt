package io.j3mobile.ui.components.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.j3mobile.protocol.OrchestrationThreadActivity
import io.j3mobile.ui.theme.J3DarkCard
import io.j3mobile.ui.theme.J3Error
import io.j3mobile.ui.theme.J3OnDarkSecondary
import io.j3mobile.ui.theme.J3Primary
import io.j3mobile.ui.theme.J3Secondary
import io.j3mobile.ui.theme.J3Warning

@Composable
fun ActivityItem(activity: OrchestrationThreadActivity) {
    val accentColor = when (activity.tone) {
        "success" -> J3Secondary
        "warning" -> J3Warning
        "error" -> J3Error
        else -> J3Primary // "info" or default
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(J3DarkCard.copy(alpha = 0.5f))
            .padding(start = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Colored left accent bar
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .width(3.dp)
                .padding(vertical = 4.dp)
                .background(accentColor),
        )

        Text(
            text = activity.summary,
            style = MaterialTheme.typography.bodySmall,
            color = J3OnDarkSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}
