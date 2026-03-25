package io.j3mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.j3mobile.domain.OrchestrationStore
import io.j3mobile.domain.ThreadRepository
import io.j3mobile.protocol.OrchestrationThread
import io.j3mobile.protocol.ProjectId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ThreadViewModel(
    private val projectId: ProjectId,
    private val store: OrchestrationStore,
    private val threadRepo: ThreadRepository,
) : ViewModel() {
    val threads: StateFlow<List<OrchestrationThread>> =
        store.threadsForProject(projectId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
