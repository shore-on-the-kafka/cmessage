package me.chacham.cmessage.integrationtest

import com.navercorp.fixturemonkey.FixtureMonkey
import com.navercorp.fixturemonkey.kotlin.KotlinPlugin
import com.navercorp.fixturemonkey.kotlin.giveMeKotlinBuilder
import me.chacham.cmessage.api.auth.*
import me.chacham.cmessage.api.group.CreateGroupRequest
import me.chacham.cmessage.api.group.GroupController
import me.chacham.cmessage.api.user.UserController
import me.chacham.cmessage.common.config.JwtService
import me.chacham.cmessage.common.config.SecurityConfig
import me.chacham.cmessage.group.infra.InMemoryGroupRepository
import me.chacham.cmessage.user.domain.User
import me.chacham.cmessage.user.domain.UserId
import me.chacham.cmessage.user.infra.InMemoryUserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.restdocs.operation.preprocess.Preprocessors.*
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters
import kotlin.test.Test

@WebFluxTest(UserController::class)
@Import(
    AuthController::class, SecurityConfig::class, InMemoryUserRepository::class, JwtService::class,
    GroupController::class, InMemoryGroupRepository::class,
)
@AutoConfigureRestDocs
class UserIntegrationTest {
    private val fm = FixtureMonkey.builder().plugin(KotlinPlugin()).build()

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    fun `getMe responses 200 OK with my information as body`() {
        // Given
        val user = register()
        val token = login(user)

        // When, Then
        webTestClient.get()
            .uri("/api/v1/users/me")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .consumeWith(
                document(
                    "get-me",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    responseFields(
                        fieldWithPath("id").description("User ID"),
                        fieldWithPath("username").description("Username"),
                    ),
                )
            )
    }

    @Test
    fun `getMyGroups responses 200 OK with my groups as body`() {
        // Given
        val user = register()
        val createGroupRequests = fm.giveMeKotlinBuilder<CreateGroupRequest>().sampleList(2)
        for (request in createGroupRequests) {
            webTestClient.post()
                .uri("/api/v1/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer ${login(user)}")
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isCreated
        }

        // When, Then
        webTestClient.get()
            .uri("/api/v1/users/me/groups")
            .header("Authorization", "Bearer ${login(user)}")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .consumeWith(
                document(
                    "create-user",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    responseFields(
                        fieldWithPath("[].id").description("Group ID"),
                        fieldWithPath("[].name").description("Group name"),
                        fieldWithPath("[].members").description("Group members ID"),
                    ),
                )
            )
    }

    private fun register(): User {
        val registerRequest = fm.giveMeKotlinBuilder<RegisterRequest>().sample()
        val response = webTestClient.post()
            .uri("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(registerRequest))
            .exchange()
            .expectStatus().isOk
            .expectBody<RegisterSuccessResponse>()
            .returnResult()
            .responseBody!!
        return User(
            id = UserId(response.id),
            username = response.username,
            password = registerRequest.password,
        )
    }

    private fun login(user: User): String {
        val loginRequest = LoginRequest(user.username, user.password)
        return webTestClient.post()
            .uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(loginRequest))
            .exchange()
            .expectStatus().isOk
            .expectBody<LoginSuccessResponse>()
            .returnResult()
            .responseBody!!.token
    }
}
