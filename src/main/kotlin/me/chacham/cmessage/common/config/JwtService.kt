package me.chacham.cmessage.common.config

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
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

    fun generateToken(username: String): String {
        val now = Instant.now()
        val expiration = now.plus(Duration.ofHours(1))

        return JWT.create()
            .withIssuer("cmessage")
            .withSubject(username)
            .withIssuedAt(now)
            .withExpiresAt(expiration)
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
