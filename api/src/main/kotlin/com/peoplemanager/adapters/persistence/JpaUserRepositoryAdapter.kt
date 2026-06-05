package com.peoplemanager.adapters.persistence

import com.peoplemanager.application.port.output.UserRepository
import com.peoplemanager.domain.OidcIdentity
import com.peoplemanager.domain.User
import com.peoplemanager.domain.UserId
import org.springframework.stereotype.Repository

@Repository
class JpaUserRepositoryAdapter(
    private val springDataUserRepository: SpringDataUserRepository
) : UserRepository {

    override fun findByOidcIdentity(oidcIdentity: OidcIdentity): User? {
        return springDataUserRepository.findByOidcSubjectAndOidcIssuer(
            oidcIdentity.subject, oidcIdentity.issuer
        )?.toDomain()
    }

    override fun save(user: User): User {
        val entity = user.toEntity()
        return springDataUserRepository.save(entity).toDomain()
    }

    override fun findAllUserIds(): List<UserId> {
        return springDataUserRepository.findAll().map { UserId(it.id) }
    }

    private fun UserEntity.toDomain(): User = User(
        id = UserId(this.id),
        oidcSubject = this.oidcSubject,
        oidcIssuer = this.oidcIssuer,
        displayName = this.displayName,
        email = this.email,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )

    private fun User.toEntity(): UserEntity = UserEntity(
        id = this.id.value,
        oidcSubject = this.oidcSubject,
        oidcIssuer = this.oidcIssuer,
        displayName = this.displayName,
        email = this.email,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}
