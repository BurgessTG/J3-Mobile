package io.j3mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.j3mobile.domain.OrchestrationStore
import io.j3mobile.domain.ThreadRepository
import io.j3mobile.protocol.OrchestrationProject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ProjectListViewModel(
    private val store: OrchestrationStore,
    private val threadRepo: ThreadRepository,
) : ViewModel() {
    val projects: StateFlow<List<OrchestrationProject>> =
        store.projects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
