package com.peoplemanager.adapters.auth

import com.peoplemanager.application.UserProvisioningService
import com.peoplemanager.domain.UserId
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

@Component
class UserProvisioningJwtAuthenticationConverter(
    private val userProvisioningService: UserProvisioningService
) : Converter<Jwt, AbstractAuthenticationToken> {

    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        val subject = jwt.subject
            ?: throw IllegalArgumentException("JWT missing subject claim")
        val issuer = jwt.issuer?.toString()
            ?: throw IllegalArgumentException("JWT missing issuer claim")
        val displayName = jwt.getClaimAsString("name")
            ?: jwt.getClaimAsString("preferred_username")
        val email = jwt.getClaimAsString("email")

        val user = userProvisioningService.provisionUser(subject, issuer, displayName, email)

        val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
        val token = JwtAuthenticationToken(jwt, authorities, subject)
        token.details = user.id
        return token
    }
}
