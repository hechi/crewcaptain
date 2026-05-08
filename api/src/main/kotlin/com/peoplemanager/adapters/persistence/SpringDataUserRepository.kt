package com.peoplemanager.adapters.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SpringDataUserRepository : JpaRepository<UserEntity, UUID> {
    fun findByOidcSubjectAndOidcIssuer(oidcSubject: String, oidcIssuer: String): UserEntity?
}
