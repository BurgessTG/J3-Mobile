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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.j3mobile.di.ConnectionManager
import io.j3mobile.protocol.ProjectId
import io.j3mobile.protocol.ThreadId
import io.j3mobile.ui.components.NewThreadSheet
import io.j3mobile.ui.components.ThreadListItem
import io.j3mobile.ui.theme.J3OnDarkSecondary
import io.j3mobile.ui.theme.J3Primary
import io.j3mobile.viewmodel.ThreadViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadListScreen(
    projectId: ProjectId,
    onThreadSelected: (ThreadId) -> Unit,
    onBack: () -> Unit,
) {
    val connectionManager: ConnectionManager = koinInject()
    val vm = remember(projectId) { ThreadViewModel(projectId, connectionManager) }
    val threads by vm.threads.collectAsState()
    val project by vm.project.collectAsState()
    var showNewThread by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = project?.title ?: "Threads",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewThread = true },
                containerColor = J3Primary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "New thread")
            }
        },
    ) { paddingValues ->
        if (threads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No conversations yet.",
                        style = MaterialTheme.typography.titleLarge,
                        color = J3OnDarkSecondary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Tap + to start your first conversation.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = J3OnDarkSecondary,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = threads,
                    key = { it.id.value },
                ) { thread ->
                    ThreadListItem(
                        thread = thread,
                        onClick = { onThreadSelected(thread.id) },
                    )
                }
            }
        }
    }

    // New thread bottom sheet
    if (showNewThread) {
        NewThreadSheet(
            onDismiss = { showNewThread = false },
            onCreate = { title, modelSelection ->
                vm.createThread(title, modelSelection)
                showNewThread = false
            },
        )
    }
}
