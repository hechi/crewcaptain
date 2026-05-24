package com.peoplemanager.adapters.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface SpringDataPersonRepository : JpaRepository<PersonEntity, UUID> {
    @Query("SELECT p FROM PersonEntity p WHERE p.id = :id AND p.userId = :userId AND p.deletedAt IS NULL")
    fun findByIdAndUserId(@Param("id") id: UUID, @Param("userId") userId: UUID): PersonEntity?

    @Query("SELECT p FROM PersonEntity p WHERE p.userId = :userId AND p.deletedAt IS NULL")
    fun findAllByUserId(@Param("userId") userId: UUID, pageable: Pageable): Page<PersonEntity>

    @Query("SELECT p FROM PersonEntity p WHERE p.userId = :userId AND p.deletedAt IS NULL")
    fun findAllByUserId(@Param("userId") userId: UUID): List<PersonEntity>

    // Standard delete that triggers JPA cascade (used for permanent deletion)
    fun deleteByIdAndUserId(id: UUID, userId: UUID): Long

    @Query("SELECT p FROM PersonEntity p WHERE p.userId = :userId AND p.moraleStatus = :moraleStatus AND p.deletedAt IS NULL")
    fun findAllByUserIdAndMoraleStatus(
        @Param("userId") userId: UUID,
        @Param("moraleStatus") moraleStatus: String,
        pageable: Pageable
    ): Page<PersonEntity>

    @Query(
        value = "SELECT * FROM persons p WHERE p.user_id = :userId AND :tag = ANY(p.tags) AND p.deleted_at IS NULL",
        nativeQuery = true
    )
    fun findAllByUserIdAndTag(
        @Param("userId") userId: UUID,
        @Param("tag") tag: String,
        pageable: Pageable
    ): Page<PersonEntity>

    @Query(
        value = "SELECT * FROM persons p WHERE p.user_id = :userId AND :tag = ANY(p.tags) AND p.morale_status = :moraleStatus AND p.deleted_at IS NULL",
        nativeQuery = true
    )
    fun findAllByUserIdAndTagAndMoraleStatus(
        @Param("userId") userId: UUID,
        @Param("tag") tag: String,
        @Param("moraleStatus") moraleStatus: String,
        pageable: Pageable
    ): Page<PersonEntity>

    // Soft-delete: find by id including deleted records (for restore)
    @Query("SELECT p FROM PersonEntity p WHERE p.id = :id AND p.userId = :userId AND p.deletedAt IS NOT NULL")
    fun findDeletedByIdAndUserId(@Param("id") id: UUID, @Param("userId") userId: UUID): PersonEntity?

    // List deleted records (trash view)
    @Query("SELECT p FROM PersonEntity p WHERE p.userId = :userId AND p.deletedAt IS NOT NULL ORDER BY p.deletedAt DESC")
    fun findAllDeletedByUserId(@Param("userId") userId: UUID, pageable: Pageable): Page<PersonEntity>

    // Soft-delete operation
    @Modifying
    @Query("UPDATE PersonEntity p SET p.deletedAt = :deletedAt, p.updatedAt = :deletedAt WHERE p.id = :id AND p.userId = :userId AND p.deletedAt IS NULL")
    fun softDeleteByIdAndUserId(@Param("id") id: UUID, @Param("userId") userId: UUID, @Param("deletedAt") deletedAt: Instant): Int

    // Restore operation
    @Modifying
    @Query("UPDATE PersonEntity p SET p.deletedAt = NULL, p.updatedAt = :now WHERE p.id = :id AND p.userId = :userId AND p.deletedAt IS NOT NULL")
    fun restoreByIdAndUserId(@Param("id") id: UUID, @Param("userId") userId: UUID, @Param("now") now: Instant): Int

    // Workspace-filtered queries
    @Query("SELECT p FROM PersonEntity p WHERE p.userId = :userId AND p.workspaceId = :workspaceId AND p.deletedAt IS NULL")
    fun findAllByUserIdAndWorkspace(@Param("userId") userId: UUID, @Param("workspaceId") workspaceId: UUID, pageable: Pageable): Page<PersonEntity>

    @Query(
        value = "SELECT * FROM persons p WHERE p.user_id = :userId AND p.workspace_id = :workspaceId AND :tag = ANY(p.tags) AND p.deleted_at IS NULL",
        nativeQuery = true
    )
    fun findAllByUserIdAndWorkspaceAndTag(
        @Param("userId") userId: UUID,
        @Param("workspaceId") workspaceId: UUID,
        @Param("tag") tag: String,
        pageable: Pageable
    ): Page<PersonEntity>

    @Query("SELECT p FROM PersonEntity p WHERE p.userId = :userId AND p.workspaceId = :workspaceId AND p.moraleStatus = :moraleStatus AND p.deletedAt IS NULL")
    fun findAllByUserIdAndWorkspaceAndMoraleStatus(
        @Param("userId") userId: UUID,
        @Param("workspaceId") workspaceId: UUID,
        @Param("moraleStatus") moraleStatus: String,
        pageable: Pageable
    ): Page<PersonEntity>

    @Query(
        value = "SELECT * FROM persons p WHERE p.user_id = :userId AND p.workspace_id = :workspaceId AND :tag = ANY(p.tags) AND p.morale_status = :moraleStatus AND p.deleted_at IS NULL",
        nativeQuery = true
    )
    fun findAllByUserIdAndWorkspaceAndTagAndMoraleStatus(
        @Param("userId") userId: UUID,
        @Param("workspaceId") workspaceId: UUID,
        @Param("tag") tag: String,
        @Param("moraleStatus") moraleStatus: String,
        pageable: Pageable
    ): Page<PersonEntity>
}
