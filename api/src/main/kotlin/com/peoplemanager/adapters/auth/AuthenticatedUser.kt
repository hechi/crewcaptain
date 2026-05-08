package com.peoplemanager.adapters.auth

import com.peoplemanager.domain.UserId
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

object AuthenticatedUser {

    fun getUserId(): UserId {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("No authentication in security context")

        val jwtAuth = authentication as? JwtAuthenticationToken
            ?: throw IllegalStateException("Authentication is not JWT-based")

        return jwtAuth.details as? UserId
            ?: throw IllegalStateException("UserId not found in authentication details")
    }
}
