package com.peoplemanager.adapters.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface SpringDataWorkspaceRepository : JpaRepository<WorkspaceEntity, UUID> {
    @Query("SELECT w FROM WorkspaceEntity w WHERE w.id = :id AND w.userId = :userId")
    fun findByIdAndUserId(@Param("id") id: UUID, @Param("userId") userId: UUID): WorkspaceEntity?

    @Query("SELECT w FROM WorkspaceEntity w WHERE w.userId = :userId ORDER BY w.displayOrder ASC, w.name ASC")
    fun findAllByUserId(@Param("userId") userId: UUID): List<WorkspaceEntity>

    fun deleteByIdAndUserId(id: UUID, userId: UUID): Long

    @Query("SELECT COUNT(w) FROM WorkspaceEntity w WHERE w.userId = :userId")
    fun countByUserId(@Param("userId") userId: UUID): Int
}
