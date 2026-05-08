package com.peoplemanager.adapters.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface SpringDataPersonRepository : JpaRepository<PersonEntity, UUID> {
    fun findByIdAndUserId(id: UUID, userId: UUID): PersonEntity?
    fun findAllByUserId(userId: UUID, pageable: Pageable): Page<PersonEntity>
    fun deleteByIdAndUserId(id: UUID, userId: UUID): Long

    @Query("SELECT p FROM PersonEntity p WHERE p.userId = :userId AND p.moraleStatus = :moraleStatus")
    fun findAllByUserIdAndMoraleStatus(
        @Param("userId") userId: UUID,
        @Param("moraleStatus") moraleStatus: String,
        pageable: Pageable
    ): Page<PersonEntity>

    @Query(
        value = "SELECT * FROM persons p WHERE p.user_id = :userId AND :tag = ANY(p.tags)",
        nativeQuery = true
    )
    fun findAllByUserIdAndTag(
        @Param("userId") userId: UUID,
        @Param("tag") tag: String,
        pageable: Pageable
    ): Page<PersonEntity>

    @Query(
        value = "SELECT * FROM persons p WHERE p.user_id = :userId AND :tag = ANY(p.tags) AND p.morale_status = :moraleStatus",
        nativeQuery = true
    )
    fun findAllByUserIdAndTagAndMoraleStatus(
        @Param("userId") userId: UUID,
        @Param("tag") tag: String,
        @Param("moraleStatus") moraleStatus: String,
        pageable: Pageable
    ): Page<PersonEntity>
}
