package com.peoplemanager.application.ports

import com.peoplemanager.domain.OidcIdentity
import com.peoplemanager.domain.User

interface UserRepository {
    fun findByOidcIdentity(oidcIdentity: OidcIdentity): User?
    fun save(user: User): User
}
