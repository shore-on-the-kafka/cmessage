package me.chacham.cmessage.integrationtest

import com.navercorp.fixturemonkey.FixtureMonkey
import com.navercorp.fixturemonkey.kotlin.KotlinPlugin
import com.navercorp.fixturemonkey.kotlin.giveMeKotlinBuilder
import me.chacham.cmessage.api.auth.AuthController
import me.chacham.cmessage.api.auth.LoginRequest
import me.chacham.cmessage.api.auth.LoginSuccessResponse
import me.chacham.cmessage.api.auth.RegisterRequest
import me.chacham.cmessage.api.group.CreateGroupRequest
import me.chacham.cmessage.api.group.CreateGroupResponse
import me.chacham.cmessage.api.group.GroupController
import me.chacham.cmessage.api.user.UserController
import me.chacham.cmessage.common.config.JwtService
import me.chacham.cmessage.common.config.SecurityConfig
import me.chacham.cmessage.group.infra.InMemoryGroupRepository
import me.chacham.cmessage.user.infra.InMemoryUserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.restdocs.operation.preprocess.Preprocessors.*
import org.springframework.restdocs.payload.PayloadDocumentation.*
import org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters

@WebFluxTest(GroupController::class)
@Import(
    AuthController::class, SecurityConfig::class, InMemoryUserRepository::class, JwtService::class,
    UserController::class, InMemoryGroupRepository::class
)
@AutoConfigureRestDocs
class GroupIntegrationTest {
    private val fm = FixtureMonkey.builder().plugin(KotlinPlugin()).build()

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    fun `createGroup responses 201 Created with created resource as body and Location header`() {
        // Given
        val request = fm.giveMeKotlinBuilder<CreateGroupRequest>()
            .setExp(CreateGroupRequest::members, listOf("otherMemberId"))
            .sample()
        val baseUrl = "http://localhost:8080"
        val token = registerAndLogin()

        // When, Then
        webTestClient.post()
            .uri("/api/v1/groups")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer $token")
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus().isCreated
            .expectHeader().value(HttpHeaders.LOCATION) { it.startsWith(baseUrl) }
            .expectBody()
            .consumeWith(
                document(
                    "create-group",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    requestFields(
                        fieldWithPath("name").description("Group name"),
                        fieldWithPath("members").description("Group members"),
                    ),
                    responseFields(
                        fieldWithPath("groupId").description("Group ID"),
                    ),
                )
            )
    }

    @Test
    fun `getGroup responses 200 OK with group as body`() {
        // Given
        val request = fm.giveMeKotlinBuilder<CreateGroupRequest>().sample()
        val token = registerAndLogin()

        // When
        val createdGroupId = webTestClient.post()
            .uri("/api/v1/groups")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer $token")
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectBody<CreateGroupResponse>()
            .returnResult()
            .responseBody!!.groupId

        // Then
        webTestClient.get()
            .uri("/api/v1/groups/{groupId}", createdGroupId.id)
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .consumeWith(
                document(
                    "get-group",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    responseFields(
                        fieldWithPath("id").description("Group ID"),
                        fieldWithPath("name").description("Group name"),
                        fieldWithPath("members").description("Group members"),
                    ),
                )
            )
    }

    @Test
    fun `addMembers responses 201 Created with Location header`() {
        // Given
        val createGroupRequest = fm.giveMeKotlinBuilder<CreateGroupRequest>().sample()
        val token = registerAndLogin()

        // When
        val createdGroupId = webTestClient.post()
            .uri("/api/v1/groups")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer $token")
            .body(BodyInserters.fromValue(createGroupRequest))
            .exchange()
            .expectBody<CreateGroupResponse>()
            .returnResult()
            .responseBody!!.groupId

        // Then
        val request = mapOf("users" to listOf("test-user-id"))
        webTestClient.post()
            .uri("/api/v1/groups/{groupId}/members", createdGroupId.id)
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus().isCreated
            .expectHeader().value(HttpHeaders.LOCATION) { it.startsWith("http://localhost:8080") }
            .expectBody()
            .consumeWith(
                document(
                    "add-members",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    requestFields(
                        fieldWithPath("users").description("Users to add"),
                    ),
                    responseFields(
                        fieldWithPath("groupId").description("Group ID"),
                        fieldWithPath("members").description("Members added"),
                    ),
                )
            )
    }

    @Test
    fun `getMembers responses 200 OK with members as body`() {
        // Given
        val createGroupRequest = fm.giveMeKotlinBuilder<CreateGroupRequest>().sample()
        val token = registerAndLogin()

        // When
        val createdGroupId = webTestClient.post()
            .uri("/api/v1/groups")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer $token")
            .body(BodyInserters.fromValue(createGroupRequest))
            .exchange()
            .expectBody<CreateGroupResponse>()
            .returnResult()
            .responseBody!!.groupId

        // Then
        webTestClient.get()
            .uri("/api/v1/groups/{groupId}/members", createdGroupId.id)
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .consumeWith(
                document(
                    "get-members",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    responseFields(
                        fieldWithPath("[]").description("User IDs"),
                    ),
                )
            )
    }

    private fun registerAndLogin(): String {
        val registerRequest = fm.giveMeKotlinBuilder<RegisterRequest>().sample()
        webTestClient.post()
            .uri("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(registerRequest))
            .exchange()
            .expectStatus().isOk
        val loginRequest = LoginRequest(registerRequest.username, registerRequest.password)
        return webTestClient.post()
            .uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(loginRequest))
            .exchange()
            .expectStatus().isOk
            .expectBody<LoginSuccessResponse>()
            .returnResult()
            .responseBody!!
            .token
    }
}
