package com.peoplemanager.adapters.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SpringDataKudosRepository : JpaRepository<KudosEntity, UUID> {

    fun findByIdAndUserIdAndPersonId(id: UUID, userId: UUID, personId: UUID): KudosEntity?

    fun findAllByUserIdAndPersonId(userId: UUID, personId: UUID, pageable: Pageable): Page<KudosEntity>

    fun findAllByUserId(userId: UUID, pageable: Pageable): Page<KudosEntity>

    fun deleteByIdAndUserIdAndPersonId(id: UUID, userId: UUID, personId: UUID): Long
}
