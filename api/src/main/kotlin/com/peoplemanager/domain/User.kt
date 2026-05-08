package com.peoplemanager.domain

import java.time.Instant

data class User(
    val id: UserId,
    val oidcSubject: String,
    val oidcIssuer: String,
    val displayName: String? = null,
    val email: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    init {
        require(oidcSubject.isNotBlank()) { "OIDC subject must not be blank" }
        require(oidcIssuer.isNotBlank()) { "OIDC issuer must not be blank" }
    }
}
