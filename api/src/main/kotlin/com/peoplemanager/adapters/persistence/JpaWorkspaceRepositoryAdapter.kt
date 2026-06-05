package com.peoplemanager.adapters.persistence

import com.peoplemanager.application.port.output.WorkspaceRepository
import com.peoplemanager.domain.UserId
import com.peoplemanager.domain.Workspace
import com.peoplemanager.domain.WorkspaceId
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class JpaWorkspaceRepositoryAdapter(
    private val springDataWorkspaceRepository: SpringDataWorkspaceRepository
) : WorkspaceRepository {

    override fun save(workspace: Workspace): Workspace {
        val entity = workspace.toEntity()
        return springDataWorkspaceRepository.save(entity).toDomain()
    }

    override fun findByIdAndUserId(workspaceId: WorkspaceId, userId: UserId): Workspace? {
        return springDataWorkspaceRepository.findByIdAndUserId(workspaceId.value, userId.value)?.toDomain()
    }

    override fun findAllByUserId(userId: UserId): List<Workspace> {
        return springDataWorkspaceRepository.findAllByUserId(userId.value).map { it.toDomain() }
    }

    override fun deleteByIdAndUserId(workspaceId: WorkspaceId, userId: UserId): Boolean {
        val deleted = springDataWorkspaceRepository.deleteByIdAndUserId(workspaceId.value, userId.value)
        return deleted > 0
    }

    override fun countByUserId(userId: UserId): Int {
        return springDataWorkspaceRepository.countByUserId(userId.value)
    }

    private fun WorkspaceEntity.toDomain(): Workspace = Workspace(
        id = WorkspaceId(this.id),
        userId = UserId(this.userId),
        name = this.name,
        description = this.description,
        displayOrder = this.displayOrder,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )

    private fun Workspace.toEntity(): WorkspaceEntity = WorkspaceEntity(
        id = this.id.value,
        userId = this.userId.value,
        name = this.name,
        description = this.description,
        displayOrder = this.displayOrder,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}
