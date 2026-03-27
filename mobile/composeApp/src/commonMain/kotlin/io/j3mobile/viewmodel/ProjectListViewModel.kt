package io.j3mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.j3mobile.di.ConnectionManager
import io.j3mobile.protocol.OrchestrationProject
import io.j3mobile.protocol.ProjectId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProjectListViewModel(
    private val connectionManager: ConnectionManager,
) : ViewModel() {
    val projects: StateFlow<List<OrchestrationProject>> =
        connectionManager.store.projects
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createProject(title: String, workspaceRoot: String) {
        val trimmedTitle = title.trim()
        val trimmedWorkspaceRoot = workspaceRoot.trim()
        if (trimmedTitle.isEmpty() || trimmedWorkspaceRoot.isEmpty()) return

        viewModelScope.launch {
            val threadRepo = connectionManager.threadRepo ?: return@launch
            threadRepo.createProject(
                id = ProjectId(generateId("project")),
                title = trimmedTitle,
                workspaceRoot = trimmedWorkspaceRoot,
            )
        }
    }

    private fun generateId(prefix: String): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        val suffix = (1..12).map { chars.random() }.joinToString("")
        return "$prefix-$suffix"
    }
}
