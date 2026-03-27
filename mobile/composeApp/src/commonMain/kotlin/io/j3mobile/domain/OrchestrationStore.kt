package io.j3mobile.domain

import io.j3mobile.protocol.OrchestrationProject
import io.j3mobile.protocol.OrchestrationReadModel
import io.j3mobile.protocol.OrchestrationThread
import io.j3mobile.protocol.ProjectId
import io.j3mobile.protocol.ThreadId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class OrchestrationStore {
    private val _readModel = MutableStateFlow<OrchestrationReadModel?>(null)
    val readModel: StateFlow<OrchestrationReadModel?> = _readModel.asStateFlow()

    fun loadSnapshot(model: OrchestrationReadModel) {
        _readModel.value = model.copy(
            projects = model.projects.filter { it.deletedAt == null },
            threads = model.threads.filter { it.deletedAt == null },
        )
    }

    fun applyEventRefreshOnly() {
        // Snapshot refreshes are the source of truth for the mobile client.
        // Event payloads are used only to trigger a refetch, not to mutate
        // the model incrementally in the app.
    }

    val projects: Flow<List<OrchestrationProject>>
        get() = _readModel.map { it?.projects.orEmpty() }

    val threads: Flow<List<OrchestrationThread>>
        get() = _readModel.map { it?.threads.orEmpty() }

    fun threadsForProject(projectId: ProjectId): Flow<List<OrchestrationThread>> =
        threads.map { list -> list.filter { it.projectId == projectId } }

    fun threadById(threadId: ThreadId): Flow<OrchestrationThread?> =
        threads.map { list -> list.find { it.id == threadId } }

    fun projectById(projectId: ProjectId): Flow<OrchestrationProject?> =
        projects.map { list -> list.find { it.id == projectId } }
}
