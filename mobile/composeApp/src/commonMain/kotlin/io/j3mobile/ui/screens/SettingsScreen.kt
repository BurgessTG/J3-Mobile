package io.j3mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.j3mobile.di.ConnectionManager
import io.j3mobile.network.ConnectionState
import io.j3mobile.ui.theme.J3DarkSurface
import io.j3mobile.ui.theme.J3Error
import io.j3mobile.ui.theme.J3OnDarkSecondary
import io.j3mobile.ui.theme.J3Secondary
import io.j3mobile.ui.theme.J3Warning
import io.j3mobile.viewmodel.SettingsViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onSaved: () -> Unit = {},
) {
    val connectionManager: ConnectionManager = koinInject()
    val vm = remember { SettingsViewModel(connectionManager) }
    val connectionState by vm.connectionState.collectAsState()
    val bridgeUrl by vm.bridgeUrl.collectAsState()
    val authToken by vm.authToken.collectAsState()
    val hasSavedSettings by vm.hasSavedSettings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = J3DarkSurface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val dotColor = when (connectionState) {
                        is ConnectionState.Connected -> J3Secondary
                        is ConnectionState.Connecting,
                        is ConnectionState.Reconnecting -> J3Warning
                        else -> J3Error
                    }
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(dotColor),
                    )
                    Text(
                        text = when (connectionState) {
                            is ConnectionState.Connected -> "Connected"
                            is ConnectionState.Connecting -> "Connecting..."
                            is ConnectionState.Reconnecting -> "Reconnecting..."
                            is ConnectionState.Disconnected -> "Disconnected"
                            is ConnectionState.Error ->
                                "Error: ${(connectionState as ConnectionState.Error).message}"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            OutlinedTextField(
                value = bridgeUrl,
                onValueChange = { vm.updateBridgeUrl(it) },
                label = { Text("Bridge URL") },
                placeholder = { Text("http://127.0.0.1:8181") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.labelSmall,
            )

            OutlinedTextField(
                value = authToken,
                onValueChange = { vm.updateAuthToken(it) },
                label = { Text("Pairing Token") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )

            FilledTonalButton(
                onClick = {
                    vm.saveAndConnect()
                    onSaved()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = bridgeUrl.isNotBlank() && authToken.isNotBlank(),
            ) {
                Text("Save and Connect")
            }

            if (hasSavedSettings || connectionState !is ConnectionState.Disconnected) {
                TextButton(
                    onClick = { vm.clearSavedConnection() },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Disconnect and Forget", color = J3Error)
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "J3 Mobile v0.1.0",
                style = MaterialTheme.typography.bodySmall,
                color = J3OnDarkSecondary,
            )
            Text(
                text = "Powered by T3 Code",
                style = MaterialTheme.typography.bodySmall,
                color = J3OnDarkSecondary,
            )
        }
    }
}
