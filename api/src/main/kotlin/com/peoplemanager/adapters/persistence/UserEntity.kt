package com.peoplemanager.adapters.persistence

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users")
class UserEntity(
    @Id
    @Column(name = "id")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "oidc_subject", nullable = false)
    val oidcSubject: String = "",

    @Column(name = "oidc_issuer", nullable = false)
    val oidcIssuer: String = "",

    @Column(name = "display_name")
    val displayName: String? = null,

    @Column(name = "email")
    val email: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
)
