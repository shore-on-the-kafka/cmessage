package me.chacham.cmessage.integrationtest

import com.navercorp.fixturemonkey.FixtureMonkey
import com.navercorp.fixturemonkey.kotlin.KotlinPlugin
import com.navercorp.fixturemonkey.kotlin.giveMeKotlinBuilder
import kotlinx.coroutines.runBlocking
import me.chacham.cmessage.api.auth.*
import me.chacham.cmessage.auth.domain.UserInfo
import me.chacham.cmessage.auth.service.AuthService
import me.chacham.cmessage.common.config.JwtService
import me.chacham.cmessage.common.config.SecurityConfig
import me.chacham.cmessage.user.infra.InMemoryUserRepository
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders
import org.springframework.restdocs.operation.preprocess.Preprocessors.*
import org.springframework.restdocs.payload.PayloadDocumentation.*
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters
import kotlin.test.Test


@WebFluxTest(AuthController::class)
@Import(SecurityConfig::class, InMemoryUserRepository::class)
@AutoConfigureRestDocs
class AuthIntegrationTest {
    private val fm = FixtureMonkey.builder().plugin(KotlinPlugin()).build()

    @MockitoBean
    private lateinit var authService: AuthService

    @MockitoBean
    private lateinit var jwtService: JwtService

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

        val expectedToken = "mocked-jwt-token-for-login"
        given(jwtService.generateToken(registerRequest.username)).willReturn(expectedToken)

        // When, Then
        webTestClient.post()
            .uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(loginRequest))
            .exchange()
            .expectStatus().isOk
            .expectBody<LoginSuccessResponse>()
            .isEqualTo(LoginSuccessResponse(expectedToken))
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

    @Test
    fun `loginWithLine redirects to Line login page`() {
        // Given
        val expectedRedirectUri =
            "https://access.line.me/oauth2/v2.1/authorize?response_type=code&client_id=test-client-id&redirect_uri=http%3A%2F%2Flocalhost%3A8080%2Fcallback&scope=profile%20openid%20email&state=12345"
        given(authService.getLineLoginUri()).willReturn(expectedRedirectUri)

        // When, Then
        webTestClient.get()
            .uri("/api/v1/auth/login/line")
            .exchange()
            .expectStatus().isFound // 302 Found
            .expectHeader().location(expectedRedirectUri)
            .expectBody()
            .consumeWith(
                document(
                    "oauth-line-login-redirect",
                    preprocessResponse(prettyPrint()),
                    responseHeaders( // 리다이렉션 응답에는 본문이 없으므로 헤더를 문서화
                        headerWithName("Location").description("URI for redirect to LINE login page")
                    )
                )
            )
    }

    @Test
    fun `loginWithLineCallback exchanges code for token and returns LoginSuccessResponse`() {
        // Given
        val requestCode = "test_authorization_code"
        val provider = "line"
        val mockUserInfo = UserInfo(provider = provider, id = "line_user_123", name = "Test Line User")
        val expectedJwtToken = "mocked-jwt-token-for-line-user"

        runBlocking { given(authService.exchangeUserInfo(provider, requestCode)).willReturn(mockUserInfo) }
        given(jwtService.generateToken(mockUserInfo.name)).willReturn(expectedJwtToken)

        // When, Then
        webTestClient.get()
            .uri { uriBuilder ->
                uriBuilder.path("/api/v1/auth/login/oauth2/code/line")
                    .queryParam("code", requestCode)
                    .build()
            }
            .exchange()
            .expectStatus().isOk
            .expectBody<LoginSuccessResponse>()
            .isEqualTo(LoginSuccessResponse(expectedJwtToken))
            .consumeWith(
                document(
                    "oauth-line-callback",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    queryParameters(
                        parameterWithName("code").description("Authorization Code from LINE"),
                    ),
                    responseFields(
                        fieldWithPath("token").description("JWT token from cmessage")
                    )
                )
            )
    }
}
