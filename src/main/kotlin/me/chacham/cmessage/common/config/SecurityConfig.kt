package me.chacham.cmessage.common.config

import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT
import kotlinx.coroutines.reactor.mono
import me.chacham.cmessage.auth.domain.JwtAuthentication
import me.chacham.cmessage.user.domain.UserId
import me.chacham.cmessage.user.repository.UserRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.AuthenticationWebFilter
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
import reactor.core.publisher.Mono

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfig(val jwtService: JwtService) {

    @Bean
    fun securityWebFilterChain(
        http: ServerHttpSecurity,
        authenticationWebFilter: AuthenticationWebFilter,
    ): SecurityWebFilterChain {
        return http
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .authorizeExchange { exchanges ->
                exchanges
                    .pathMatchers("/api/v1/auth/**").permitAll()
                    .anyExchange().authenticated()
            }
            .csrf { it.disable() }
            .addFilterAt(authenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .build()
    }

    @Bean
    fun authenticationWebFilter(
        reactiveAuthenticationManager: ReactiveAuthenticationManager,
        serverAuthenticationConverter: ServerAuthenticationConverter,
    ): AuthenticationWebFilter {
        val authenticationWebFilter = AuthenticationWebFilter(reactiveAuthenticationManager)
        authenticationWebFilter.setServerAuthenticationConverter(serverAuthenticationConverter)
        authenticationWebFilter.setAuthenticationFailureHandler(serverAuthenticationFailureHandler())
        return authenticationWebFilter
    }

    @Bean
    fun reactiveAuthenticationManager(userRepository: UserRepository): ReactiveAuthenticationManager {
        return ReactiveAuthenticationManager { authentication ->
            if (authentication !is PreAuthJwtAuthentication) {
                return@ReactiveAuthenticationManager Mono.error(BadCredentialsException("Invalid token"))
            }
            val decodedJWT = jwtService.validateAndDecodeToken(authentication.credentials)
                ?: return@ReactiveAuthenticationManager Mono.error(BadCredentialsException("Invalid token"))
            val id = UserId(decodedJWT.subject)

            mono { userRepository.find(id) }
                .map { user -> JwtAuthentication(decodedJWT, user) }
        }
    }

    @Bean
    fun serverAuthenticationConverter(): ServerAuthenticationConverter {
        return ServerAuthenticationConverter { exchange ->
            exchange.request.headers["Authorization"]?.first()?.let { token ->
                if (token.startsWith("Bearer ")) {
                    val jwt = token.substring(7)
                    Mono.just(PreAuthJwtAuthentication(jwt))
                } else {
                    Mono.empty()
                }
            } ?: Mono.empty()
        }
    }

    private fun serverAuthenticationFailureHandler(): ServerAuthenticationFailureHandler {
        return ServerAuthenticationFailureHandler { _, _ ->
            Mono.error(Exception("Invalid token"))
        }
    }

    data class PreAuthJwtAuthentication(val jwt: String) : AbstractAuthenticationToken(null) {
        override fun getCredentials(): String {
            return jwt
        }

        override fun getPrincipal(): DecodedJWT {
            return JWT.decode(jwt)
        }
    }
}
