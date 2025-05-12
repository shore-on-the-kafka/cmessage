package me.chacham.cmessage.api.auth

import me.chacham.cmessage.auth.service.AuthService
import me.chacham.cmessage.common.config.JwtService
import me.chacham.cmessage.user.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val userRepository: UserRepository,
    private val jwtService: JwtService,
    private val authService: AuthService,
) {
    @GetMapping("/login/line")
    suspend fun loginWithLine(): ResponseEntity<Unit> {
        val redirectUri = authService.getLineLoginUri()
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(redirectUri))
            .build()
    }

    @GetMapping("/login/oauth2/line")
    suspend fun loginWithLineCallback(
        @RequestParam("code") code: String,
    ): ResponseEntity<LoginResponse> {
        val userInfo = authService.exchangeUserInfo("line", code)
        val user = userRepository.find(userInfo.id) ?: userRepository.saveUser(userInfo.id, userInfo.name)
        val token = jwtService.generateToken(user)
        return ResponseEntity.ok(LoginSuccessResponse(token))
    }
}
