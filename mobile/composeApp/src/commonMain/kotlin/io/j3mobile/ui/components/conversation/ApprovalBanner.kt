package io.j3mobile.ui.components.conversation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.j3mobile.ui.theme.J3DarkCard
import io.j3mobile.ui.theme.J3Error
import io.j3mobile.ui.theme.J3OnDark
import io.j3mobile.ui.theme.J3Secondary
import io.j3mobile.ui.theme.J3Warning
import io.j3mobile.viewmodel.PendingApprovalInfo

@Composable
fun ApprovalBanner(
    info: PendingApprovalInfo,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = J3DarkCard),
        border = BorderStroke(1.dp, J3Warning),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Tool Approval Required",
                style = MaterialTheme.typography.titleSmall,
                color = J3Warning,
            )

            Text(
                text = info.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = J3OnDark,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = J3Error,
                    ),
                    border = BorderStroke(1.dp, J3Error),
                ) {
                    Text("Reject")
                }
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = J3Secondary,
                    ),
                ) {
                    Text("Approve")
                }
            }
        }
    }
}
