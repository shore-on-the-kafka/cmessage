package me.chacham.cmessage.auth.domain

import com.auth0.jwt.interfaces.DecodedJWT
import me.chacham.cmessage.user.domain.User
import me.chacham.cmessage.user.domain.UserId
import org.springframework.security.authentication.AbstractAuthenticationToken

class JwtAuthentication(
    private val decodedJWT: DecodedJWT,
    private val user: User,
) : AbstractAuthenticationToken(null) {
    fun getId(): UserId {
        return user.id
    }

    override fun getName(): String {
        return user.name
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
