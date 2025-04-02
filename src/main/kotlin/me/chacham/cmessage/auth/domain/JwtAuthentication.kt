package me.chacham.cmessage.auth.domain

import com.auth0.jwt.interfaces.DecodedJWT
import me.chacham.cmessage.user.domain.User
import org.springframework.security.authentication.AbstractAuthenticationToken

class JwtAuthentication(
    private val decodedJWT: DecodedJWT,
    private val user: User,
) : AbstractAuthenticationToken(null) {
    override fun getName(): String {
        return decodedJWT.subject
    }

    override fun getCredentials(): String {
        return decodedJWT.token
    }

    override fun getPrincipal(): User {
        return user
    }

    override fun isAuthenticated(): Boolean {
        return true
    }
}
