package me.chacham.cmessage.api.auth

import me.chacham.cmessage.common.config.JwtService
import me.chacham.cmessage.user.repository.UserRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val userRepository: UserRepository,
    private val jwtService: JwtService,
) {
    @PostMapping("/register")
    suspend fun register(@RequestBody request: RegisterRequest): ResponseEntity<RegisterResponse> {
        if (userRepository.findUserByUsername(request.username) != null) {
            return ResponseEntity.badRequest().body(RegisterFailureResponse("Username is already taken!"))
        }

        val registeredUser = userRepository.saveUser(request.username, request.password)
        return ResponseEntity.ok(
            RegisterSuccessResponse(
                id = registeredUser.id.id,
                username = registeredUser.username,
            )
        )
    }

    @PostMapping("/login")
    suspend fun login(@RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
        val user = userRepository.findUserByUsername(request.username)
            ?: return ResponseEntity.badRequest().body(LoginFailureResponse("Invalid username or password"))

        if (request.password != user.password) {
            return ResponseEntity.badRequest().body(LoginFailureResponse("Invalid username or password"))
        }

        val token = jwtService.generateToken(user.username)
        return ResponseEntity.ok(LoginSuccessResponse(token))
    }
}
