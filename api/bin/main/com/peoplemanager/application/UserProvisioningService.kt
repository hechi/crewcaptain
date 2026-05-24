package com.peoplemanager.application

import com.peoplemanager.application.ports.UserRepository
import com.peoplemanager.domain.OidcIdentity
import com.peoplemanager.domain.User
import com.peoplemanager.domain.UserId
import org.springframework.stereotype.Service

@Service
class UserProvisioningService(
    private val userRepository: UserRepository
) {
    fun provisionUser(subject: String, issuer: String, displayName: String?, email: String?): User {
        val oidcIdentity = OidcIdentity(subject = subject, issuer = issuer)

        val existingUser = userRepository.findByOidcIdentity(oidcIdentity)
        if (existingUser != null) {
            return existingUser
        }

        val newUser = User(
            id = UserId.generate(),
            oidcSubject = subject,
            oidcIssuer = issuer,
            displayName = displayName,
            email = email
        )
        return userRepository.save(newUser)
    }
}
