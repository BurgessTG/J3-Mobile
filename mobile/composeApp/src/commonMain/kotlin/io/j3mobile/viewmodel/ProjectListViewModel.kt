package io.j3mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.j3mobile.domain.OrchestrationStore
import io.j3mobile.protocol.OrchestrationProject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ProjectListViewModel(
    store: OrchestrationStore,
) : ViewModel() {
    val projects: StateFlow<List<OrchestrationProject>> =
        store.projects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
