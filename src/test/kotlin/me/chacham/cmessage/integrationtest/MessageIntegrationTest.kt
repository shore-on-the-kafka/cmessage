package me.chacham.cmessage.integrationtest

import com.navercorp.fixturemonkey.FixtureMonkey
import com.navercorp.fixturemonkey.kotlin.KotlinPlugin
import com.navercorp.fixturemonkey.kotlin.giveMeKotlinBuilder
import me.chacham.cmessage.api.auth.AuthController
import me.chacham.cmessage.api.auth.LoginSuccessResponse
import me.chacham.cmessage.api.group.CreateGroupRequest
import me.chacham.cmessage.api.group.CreateGroupResponse
import me.chacham.cmessage.api.group.GroupController
import me.chacham.cmessage.api.message.MessageController
import me.chacham.cmessage.api.message.SendMessageRequest
import me.chacham.cmessage.api.message.SendMessageResponse
import me.chacham.cmessage.auth.domain.UserInfo
import me.chacham.cmessage.auth.service.AuthService
import me.chacham.cmessage.common.config.JwtService
import me.chacham.cmessage.common.config.SecurityConfig
import me.chacham.cmessage.group.domain.GroupId
import me.chacham.cmessage.group.infra.InMemoryGroupRepository
import me.chacham.cmessage.message.domain.Message
import me.chacham.cmessage.message.infra.InMemoryMessageRepository
import me.chacham.cmessage.message.service.MessageService
import me.chacham.cmessage.testutil.FixtureMonkeyUtil.FM
import me.chacham.cmessage.user.domain.User
import me.chacham.cmessage.user.domain.UserId
import me.chacham.cmessage.user.infra.InMemoryUserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.wheneverBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.restdocs.operation.preprocess.Preprocessors.*
import org.springframework.restdocs.payload.PayloadDocumentation.*
import org.springframework.restdocs.request.RequestDocumentation.*
import org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.ListBodySpec
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters
import java.util.*

@WebFluxTest(
    MessageController::class
)
@Import(
    AuthController::class, SecurityConfig::class, InMemoryUserRepository::class, JwtService::class,
    GroupController::class, InMemoryGroupRepository::class,
    MessageService::class, InMemoryMessageRepository::class,
)
@AutoConfigureRestDocs
class MessageIntegrationTest {
    private val fm = FixtureMonkey.builder().plugin(KotlinPlugin()).build()

    @MockitoBean
    private lateinit var authService: AuthService

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    fun `sendMessage responses 201 Created with created resource as body and Location header`() {
        val sender = User(UserId(UUID.randomUUID().toString()), "sender")
        val receiver = User(UserId(UUID.randomUUID().toString()), "receiver")
        val request = fm.giveMeKotlinBuilder<SendMessageRequest>()
            .setExp(SendMessageRequest::receiverId, receiver.id)
            .setNullExp(SendMessageRequest::groupId)
            .sample()
        val baseUrl = "http://localhost:8080"

        webTestClient.post()
            .uri("/api/v1/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer ${login(sender)}")
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus().isCreated
            .expectHeader().value(HttpHeaders.LOCATION) { it.startsWith(baseUrl) }
            .expectBody()
            .consumeWith(
                document(
                    "send-messages",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    requestFields(
                        fieldWithPath("receiverId").description("receiverId").optional(),
                        fieldWithPath("groupId").description("groupId").optional(),
                        fieldWithPath("content").description("content"),
                    ),
                    responseFields(
                        fieldWithPath("messageId").description("created messageId"),
                    ),
                )
            )
    }

    @Test
    fun `getMessages responses message list`() {
        val sender = User(UserId(UUID.randomUUID().toString()), "sender")
        val receiver = User(UserId(UUID.randomUUID().toString()), "receiver")
        val groupId = GroupId(UUID.randomUUID().toString())

        val requests = fm.giveMeKotlinBuilder<SendMessageRequest>()
            .setExp(SendMessageRequest::receiverId, receiver.id)
            .setNullExp(SendMessageRequest::groupId)
            .sampleList(2)
        requests.forEach { request ->
            webTestClient.post()
                .uri("/api/v1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer ${login(sender)}")
                .body(BodyInserters.fromValue(request))
                .exchange()
        }


        val groupRequests = fm.giveMeKotlinBuilder<SendMessageRequest>()
            .setNullExp(SendMessageRequest::receiverId)
            .setExp(SendMessageRequest::groupId, groupId)
            .sampleList(2)
        groupRequests.forEach { request ->
            webTestClient.post()
                .uri("/api/v1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
        }
        val messages = webTestClient.get()
            .uri("/api/v1/messages?userIdPair={senderId}+{receiverId}", sender.id.id, receiver.id.id)
            .header("Authorization", "Bearer ${login(sender)}")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(Message::class.java)
            .consumeWith<ListBodySpec<Message>>(
                document(
                    "get-messages",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    queryParameters(
                        parameterWithName("senderId").description("senderId").optional(),
                        parameterWithName("receiverId").description("receiverId").optional(),
                        parameterWithName("userIdPair").description("userIdPair").optional(),
                        parameterWithName("groupId").description("groupId").optional(),
                    ),
                    responseFields(
                        fieldWithPath("[].messageId").description("messageId"),
                        fieldWithPath("[].senderId").description("senderId"),
                        fieldWithPath("[].receiverId").description("receiverId").optional(),
                        fieldWithPath("[].groupId").description("groupId").optional(),
                        fieldWithPath("[].content").description("content"),
                    )
                )
            )
            .returnResult()
            .responseBody!!

        assertEquals(2, messages.size)
    }

    @Test
    fun `getMessages responses message list with receiverId`() {
        val sender = User(UserId(UUID.randomUUID().toString()), "sender")
        val receiver = User(UserId(UUID.randomUUID().toString()), "receiver")

        val createGroupRequest = fm.giveMeKotlinBuilder<CreateGroupRequest>()
            .setExp(CreateGroupRequest::members, listOf(sender.id, receiver.id))
            .sample()
        val groupId = webTestClient.post()
            .uri("/api/v1/groups")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer ${login(sender)}")
            .body(BodyInserters.fromValue(createGroupRequest))
            .exchange()
            .expectBody<CreateGroupResponse>()
            .returnResult()
            .responseBody!!.groupId

        val sendMessageRequests = fm.giveMeKotlinBuilder<SendMessageRequest>()
            .setExp(SendMessageRequest::receiverId, receiver.id)
            .setNullExp(SendMessageRequest::groupId)
            .sampleList(2)
        sendMessageRequests.forEach { request ->
            webTestClient.post()
                .uri("/api/v1/messages")
                .header("Authorization", "Bearer ${login(sender)}")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
        }

        val groupRequests = fm.giveMeKotlinBuilder<SendMessageRequest>()
            .setNullExp(SendMessageRequest::receiverId)
            .setExp(SendMessageRequest::groupId, groupId)
            .sampleList(2)
        groupRequests.forEach { request ->
            webTestClient.post()
                .uri("/api/v1/messages")
                .header("Authorization", "Bearer ${login(sender)}")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
        }

        val messages = webTestClient.get()
            .uri("/api/v1/messages?receiverId={receiverId}", receiver.id.id)
            .header("Authorization", "Bearer ${login(sender)}")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(Message::class.java)
            .consumeWith<ListBodySpec<Message>>(
                document(
                    "get-messages",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    queryParameters(
                        parameterWithName("senderId").description("senderId").optional(),
                        parameterWithName("receiverId").description("receiverId").optional(),
                        parameterWithName("userIdPair").description("userIdPair").optional(),
                        parameterWithName("groupId").description("groupId").optional(),
                    ),
                    responseFields(
                        fieldWithPath("[].messageId").description("messageId"),
                        fieldWithPath("[].senderId").description("senderId"),
                        fieldWithPath("[].receiverId").description("receiverId").optional(),
                        fieldWithPath("[].groupId").description("groupId").optional(),
                        fieldWithPath("[].content").description("content"),
                    )
                )
            )
            .returnResult()
            .responseBody!!
        assertEquals(4, messages.size)
    }

    @Test
    fun `getMessage responses found message`() {
        val sender = User(UserId(UUID.randomUUID().toString()), "sender")
        val receiver = User(UserId(UUID.randomUUID().toString()), "receiver")

        val request = fm.giveMeKotlinBuilder<SendMessageRequest>()
            .setExp(SendMessageRequest::receiverId, receiver.id)
            .setNullExp(SendMessageRequest::groupId)
            .sample()
        val responseMessageId = webTestClient.post()
            .uri("/api/v1/messages")
            .header("Authorization", "Bearer ${login(sender)}")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectBody<SendMessageResponse>()
            .returnResult()
            .responseBody!!.messageId

        webTestClient.get()
            .uri("/api/v1/messages/{messageId}", responseMessageId.id)
            .header("Authorization", "Bearer ${login(sender)}")
            .exchange()
            .expectStatus().isOk
            .expectBody(Message::class.java)
            .consumeWith(
                document(
                    "get-message",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    pathParameters(
                        parameterWithName("messageId").description("messageId"),
                    ),
                    responseFields(
                        fieldWithPath("messageId").description("messageId"),
                        fieldWithPath("senderId").description("senderId"),
                        fieldWithPath("receiverId").description("receiverId").optional(),
                        fieldWithPath("groupId").description("groupId").optional(),
                        fieldWithPath("content").description("content"),
                    )
                )
            )
    }

    private fun login(user: User): String {
        val userInfo = FM.giveMeKotlinBuilder<UserInfo>()
            .setExp(UserInfo::provider, "test")
            .setExp(UserInfo::id, user.id)
            .setExp(UserInfo::name, user.name)
            .sample()
        val code = "testcode"
        wheneverBlocking { authService.exchangeUserInfo("line", code) } doReturn userInfo
        return webTestClient.get()
            .uri { builder ->
                builder.path("/api/v1/auth/login/oauth2/line")
                    .queryParam("code", code)
                    .build()
            }
            .exchange()
            .expectStatus().isOk
            .expectBody<LoginSuccessResponse>()
            .returnResult()
            .responseBody!!
            .token
    }
}
