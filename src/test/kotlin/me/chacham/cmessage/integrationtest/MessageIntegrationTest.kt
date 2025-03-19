package me.chacham.cmessage.integrationtest

import com.navercorp.fixturemonkey.FixtureMonkey
import com.navercorp.fixturemonkey.kotlin.KotlinPlugin
import com.navercorp.fixturemonkey.kotlin.giveMeKotlinBuilder
import me.chacham.cmessage.api.group.CreateGroupRequest
import me.chacham.cmessage.api.group.CreateGroupResponse
import me.chacham.cmessage.api.group.GroupController
import me.chacham.cmessage.api.message.MessageController
import me.chacham.cmessage.api.message.SendMessageRequest
import me.chacham.cmessage.api.message.SendMessageResponse
import me.chacham.cmessage.group.domain.GroupId
import me.chacham.cmessage.group.repository.GroupRepository
import me.chacham.cmessage.message.domain.Message
import me.chacham.cmessage.message.repository.MessageRepository
import me.chacham.cmessage.message.service.MessageService
import me.chacham.cmessage.user.domain.UserId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.restdocs.operation.preprocess.Preprocessors.*
import org.springframework.restdocs.payload.PayloadDocumentation.*
import org.springframework.restdocs.request.RequestDocumentation.*
import org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.ListBodySpec
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

@WebFluxTest(
    MessageController::class,
    MessageService::class,
    MessageRepository::class,
    GroupController::class,
    GroupRepository::class
)
@AutoConfigureRestDocs
class MessageIntegrationTest {

    private val fm = FixtureMonkey.builder().plugin(KotlinPlugin()).build()

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    fun `sendMessage responses 201 Created with created resource as body and Location header`() {
        val senderId = UserId(UUID.randomUUID().toString())
        val receiverId = UserId(UUID.randomUUID().toString())
        val request = fm.giveMeKotlinBuilder<SendMessageRequest>()
            .setExp(SendMessageRequest::senderId, senderId)
            .setExp(SendMessageRequest::receiverId, receiverId)
            .setNullExp(SendMessageRequest::groupId)
            .sample()
        val baseUrl = "http://localhost:8080"

        webTestClient.post()
            .uri("/api/v1/messages")
            .contentType(MediaType.APPLICATION_JSON)
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
                        fieldWithPath("senderId").description("senderId"),
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
        val senderId = UserId(UUID.randomUUID().toString())
        val receiverId = UserId(UUID.randomUUID().toString())
        val groupId = GroupId(UUID.randomUUID().toString())

        val requests = fm.giveMeKotlinBuilder<SendMessageRequest>()
            .setExp(SendMessageRequest::senderId, senderId)
            .setExp(SendMessageRequest::receiverId, receiverId)
            .setNullExp(SendMessageRequest::groupId)
            .sampleList(2)
        requests.forEach { request ->
            webTestClient.post()
                .uri("/api/v1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
        }


        val groupRequests = fm.giveMeKotlinBuilder<SendMessageRequest>()
            .setExp(SendMessageRequest::senderId, senderId)
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
            .uri("/api/v1/messages?userIdPair=${senderId.id}+${receiverId.id}")
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
        val senderId = UserId(UUID.randomUUID().toString())
        val receiverId = UserId(UUID.randomUUID().toString())

        val createGroupRequest = fm.giveMeKotlinBuilder<CreateGroupRequest>().sample()
        val groupId = webTestClient.post()
            .uri("/api/v1/groups")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-User-Id", "test-user-id")
            .body(BodyInserters.fromValue(createGroupRequest))
            .exchange()
            .expectBody<CreateGroupResponse>()
            .returnResult()
            .responseBody!!.groupId

        val addUsersRequest = mapOf("users" to listOf(senderId, receiverId))
        webTestClient.post()
            .uri("/api/v1/groups/{groupId}/members", groupId.id)
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(addUsersRequest))
            .exchange()

        val requests = fm.giveMeKotlinBuilder<SendMessageRequest>()
            .setExp(SendMessageRequest::senderId, senderId)
            .setExp(SendMessageRequest::receiverId, receiverId)
            .setNullExp(SendMessageRequest::groupId)
            .sampleList(2)
        requests.forEach { request ->
            webTestClient.post()
                .uri("/api/v1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
        }

        val groupRequests = fm.giveMeKotlinBuilder<SendMessageRequest>()
            .setExp(SendMessageRequest::senderId, senderId)
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
            .uri("/api/v1/messages?receiverId=${receiverId.id}")
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
        val senderId = UserId(UUID.randomUUID().toString())
        val receiverId = UserId(UUID.randomUUID().toString())

        val request = fm.giveMeKotlinBuilder<SendMessageRequest>()
            .setExp(SendMessageRequest::senderId, senderId)
            .setExp(SendMessageRequest::receiverId, receiverId)
            .setNullExp(SendMessageRequest::groupId)
            .sample()
        val responseMessageId = webTestClient.post()
            .uri("/api/v1/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectBody<SendMessageResponse>()
            .returnResult()
            .responseBody!!.messageId

        webTestClient.get()
            .uri("/api/v1/messages/{messageId}", responseMessageId.id)
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
}
