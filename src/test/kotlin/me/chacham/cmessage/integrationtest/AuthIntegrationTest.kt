package me.chacham.cmessage.integrationtest

import com.navercorp.fixturemonkey.FixtureMonkey
import com.navercorp.fixturemonkey.kotlin.KotlinPlugin
import kotlinx.coroutines.runBlocking
import me.chacham.cmessage.api.auth.AuthController
import me.chacham.cmessage.api.auth.LoginSuccessResponse
import me.chacham.cmessage.auth.domain.UserInfo
import me.chacham.cmessage.auth.service.AuthService
import me.chacham.cmessage.common.config.JwtService
import me.chacham.cmessage.common.config.SecurityConfig
import me.chacham.cmessage.user.domain.User
import me.chacham.cmessage.user.domain.UserId
import me.chacham.cmessage.user.infra.InMemoryUserRepository
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders
import org.springframework.restdocs.operation.preprocess.Preprocessors.*
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
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
        val mockUserInfo = UserInfo(provider = provider, id = UserId("line_user_123"), name = "Test Line User")
        val expectedJwtToken = "mocked-jwt-token-for-line-user"

        runBlocking { given(authService.exchangeUserInfo(provider, requestCode)).willReturn(mockUserInfo) }
        given(jwtService.generateToken(User(mockUserInfo.id, mockUserInfo.name))).willReturn(expectedJwtToken)

        // When, Then
        webTestClient.get()
            .uri { uriBuilder ->
                uriBuilder.path("/api/v1/auth/login/oauth2/line")
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
