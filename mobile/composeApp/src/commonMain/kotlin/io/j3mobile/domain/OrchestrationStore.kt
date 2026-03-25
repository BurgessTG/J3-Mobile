package io.j3mobile.domain

import io.j3mobile.protocol.OrchestrationEvent
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
        _readModel.value = model
    }

    fun applyEvent(event: OrchestrationEvent) {
        val current = _readModel.value ?: return
        _readModel.value = current.copy(
            snapshotSequence = event.sequence,
            updatedAt = event.occurredAt,
        )
        // Note: Full event application (updating projects/threads/messages)
        // will be implemented incrementally. For now, the mobile client
        // re-fetches the snapshot periodically or on key events.
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
