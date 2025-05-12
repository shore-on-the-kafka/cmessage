package me.chacham.cmessage.common.config

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import me.chacham.cmessage.user.domain.User
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant

@Service
class JwtService {
    private val secret: ByteArray = ByteArray(64)

    init {
        SecureRandom().nextBytes(secret)
    }

    fun generateToken(user: User): String {
        val now = Instant.now()
        val expiration = now.plus(Duration.ofHours(1))

        return JWT.create()
            .withIssuer("cmessage")
            .withSubject(user.id.id)
            .withIssuedAt(now)
            .withExpiresAt(expiration)
            .withPayload(mapOf<String, String>("name" to user.name))
            .sign(Algorithm.HMAC256(secret))
    }

    fun validateAndDecodeToken(token: String): DecodedJWT? {
        return try {
            JWT.require(Algorithm.HMAC256(secret))
                .build()
                .verify(token)
        } catch (e: Exception) {
            null
        }
    }
}
