package io.j3mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import io.j3mobile.di.ConnectionManager
import io.j3mobile.network.ConnectionState
import io.j3mobile.ui.components.ConnectionStatusBar
import io.j3mobile.ui.components.ProjectListItem
import io.j3mobile.ui.theme.J3OnDarkSecondary
import io.j3mobile.viewmodel.ProjectListViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    onProjectSelected: (String) -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    val connectionManager: ConnectionManager = koinInject()
    val vm = remember { ProjectListViewModel(connectionManager.store) }
    val projects by vm.projects.collectAsState()
    val connectionState by connectionManager.connectionState.collectAsState()
    val isConnected = connectionState is ConnectionState.Connected

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Projects",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Connection status bar
            ConnectionStatusBar(
                connectionState = connectionState,
                onSettings = onSettingsClick,
            )

            when {
                // Not connected — prompt to go to settings
                !isConnected -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Not connected",
                                style = MaterialTheme.typography.titleLarge,
                                color = J3OnDarkSecondary,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Connect to your bridge server to see projects.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = J3OnDarkSecondary,
                            )
                            Spacer(Modifier.height(16.dp))
                            TextButton(onClick = onSettingsClick) {
                                Text("Go to Settings")
                            }
                        }
                    }
                }

                // Connected but no projects
                projects.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No projects yet.",
                                style = MaterialTheme.typography.titleLarge,
                                color = J3OnDarkSecondary,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Projects from your T3 Code\nserver will appear here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = J3OnDarkSecondary,
                            )
                        }
                    }
                }

                // Has projects — show list
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(
                            items = projects,
                            key = { it.id.value },
                        ) { project ->
                            ProjectListItem(
                                project = project,
                                onClick = { onProjectSelected(project.id.value) },
                            )
                        }
                    }
                }
            }
        }
    }
}
