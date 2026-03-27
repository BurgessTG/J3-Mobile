package io.j3mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.j3mobile.di.ConnectionManager
import io.j3mobile.protocol.ModelSelection
import io.j3mobile.protocol.OrchestrationProject
import io.j3mobile.protocol.OrchestrationThread
import io.j3mobile.protocol.ProjectId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThreadViewModel(
    private val projectId: ProjectId,
    private val connectionManager: ConnectionManager,
) : ViewModel() {
    val threads: StateFlow<List<OrchestrationThread>> =
        connectionManager.store.threadsForProject(projectId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val project: StateFlow<OrchestrationProject?> =
        connectionManager.store.projectById(projectId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun createThread(title: String, modelSelection: ModelSelection) {
        viewModelScope.launch {
            val threadRepo = connectionManager.threadRepo ?: return@launch
            val threadId = threadRepo.newThreadId()
            threadRepo.createThread(threadId, projectId, title.trim(), modelSelection)
        }
    }
}
