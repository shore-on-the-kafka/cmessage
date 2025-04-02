package me.chacham.cmessage.integrationtest

import com.navercorp.fixturemonkey.FixtureMonkey
import com.navercorp.fixturemonkey.kotlin.KotlinPlugin
import com.navercorp.fixturemonkey.kotlin.giveMeKotlinBuilder
import me.chacham.cmessage.api.auth.*
import me.chacham.cmessage.common.config.JwtService
import me.chacham.cmessage.common.config.SecurityConfig
import me.chacham.cmessage.user.infra.InMemoryUserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.restdocs.operation.preprocess.Preprocessors.*
import org.springframework.restdocs.payload.PayloadDocumentation.*
import org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters
import kotlin.test.Test

@WebFluxTest(AuthController::class)
@Import(SecurityConfig::class, InMemoryUserRepository::class, JwtService::class)
@AutoConfigureRestDocs
class AuthIntegrationTest {
    private val fm = FixtureMonkey.builder().plugin(KotlinPlugin()).build()

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    fun `register responses 200 OK with success message`() {
        // Given
        val registerRequest = fm.giveMeKotlinBuilder<RegisterRequest>().sample()

        // When, Then
        webTestClient.post()
            .uri("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(registerRequest))
            .exchange()
            .expectStatus().isOk
            .expectBody<RegisterSuccessResponse>()
            .consumeWith(
                document(
                    "register",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    requestFields(
                        fieldWithPath("username").description("Username"),
                        fieldWithPath("password").description("Password"),
                    ),
                    responseFields(
                        fieldWithPath("id").description("User ID"),
                        fieldWithPath("username").description("Username"),
                    ),
                )
            )
    }

    @Test
    fun `register responses 400 Bad Request with error message when username is already taken`() {
        // Given
        val registerRequest = fm.giveMeKotlinBuilder<RegisterRequest>().sample()
        webTestClient.post()
            .uri("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(registerRequest))
            .exchange()
            .expectStatus().isOk

        // When, Then
        webTestClient.post()
            .uri("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(registerRequest))
            .exchange()
            .expectStatus().isBadRequest
            .expectBody<RegisterFailureResponse>()
            .consumeWith(
                document(
                    "register-duplicate",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    requestFields(
                        fieldWithPath("username").description("Username"),
                        fieldWithPath("password").description("Password"),
                    ),
                    responseFields(
                        fieldWithPath("error").description("Error message"),
                    ),
                )
            )
    }

    @Test
    fun `login responses 200 OK with token`() {
        // Given
        val registerRequest = fm.giveMeKotlinBuilder<RegisterRequest>().sample()
        webTestClient.post()
            .uri("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(registerRequest))
            .exchange()
            .expectStatus().isOk

        val loginRequest = fm.giveMeKotlinBuilder<LoginRequest>()
            .setExp(LoginRequest::username, registerRequest.username)
            .setExp(LoginRequest::password, registerRequest.password)
            .sample()

        // When, Then
        webTestClient.post()
            .uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(loginRequest))
            .exchange()
            .expectStatus().isOk
            .expectBody<LoginSuccessResponse>()
            .consumeWith(
                document(
                    "login",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    requestFields(
                        fieldWithPath("username").description("Username"),
                        fieldWithPath("password").description("Password"),
                    ),
                    responseFields(
                        fieldWithPath("token").description("JWT Token"),
                    ),
                )
            )
    }

    @Test
    fun `login responses 400 Bad Request with error message when username or password is invalid`() {
        // Given
        val registerRequest = fm.giveMeKotlinBuilder<RegisterRequest>().sample()
        webTestClient.post()
            .uri("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(registerRequest))
            .exchange()
            .expectStatus().isOk

        val loginRequest = fm.giveMeKotlinBuilder<LoginRequest>()
            .setExp(LoginRequest::username, registerRequest.username)
            .setExp(LoginRequest::password, "wrongPassword")
            .sample()

        // When, Then
        webTestClient.post()
            .uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(loginRequest))
            .exchange()
            .expectStatus().isBadRequest
            .expectBody<LoginFailureResponse>()
            .consumeWith(
                document(
                    "login-invalid",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    requestFields(
                        fieldWithPath("username").description("Username"),
                        fieldWithPath("password").description("Password"),
                    ),
                    responseFields(
                        fieldWithPath("error").description("Error message"),
                    ),
                )
            )
    }
}
