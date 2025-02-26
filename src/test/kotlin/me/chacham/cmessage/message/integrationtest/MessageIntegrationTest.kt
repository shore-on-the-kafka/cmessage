package me.chacham.cmessage.message.integrationtest

import com.navercorp.fixturemonkey.FixtureMonkey
import com.navercorp.fixturemonkey.kotlin.KotlinPlugin
import com.navercorp.fixturemonkey.kotlin.giveMeKotlinBuilder
import me.chacham.cmessage.message.api.MessageController
import me.chacham.cmessage.message.api.SendMessageRequest
import me.chacham.cmessage.message.api.SendMessageResponse
import me.chacham.cmessage.message.domain.Message
import me.chacham.cmessage.message.repository.MessageRepository
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

@WebFluxTest(MessageController::class, MessageRepository::class)
@AutoConfigureRestDocs
class MessageDocumentTest {

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
                        fieldWithPath("receiverId").description("receiverId"),
                        fieldWithPath("content").description("content"),
                    ),
                    responseFields(
                        fieldWithPath("messageId").description("created messageId"),
                    ),
                )
            )
            .returnResult()
    }

    @Test
    fun `getMessages responses message list`() {
        val senderId = UserId(UUID.randomUUID().toString())
        val receiverId = UserId(UUID.randomUUID().toString())

        val requests = fm.giveMeKotlinBuilder<SendMessageRequest>()
            .setExp(SendMessageRequest::senderId, senderId)
            .setExp(SendMessageRequest::receiverId, receiverId)
            .sampleList(2)
        requests.forEach { request ->
            webTestClient.post()
                .uri("/api/v1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
        }

        val messages = webTestClient.get()
            .uri("/api/v1/messages?senderId=${senderId.id}&receiverId=${receiverId.id}")
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
                    ),
                    responseFields(
                        fieldWithPath("[].messageId").description("messageId"),
                        fieldWithPath("[].senderId").description("senderId"),
                        fieldWithPath("[].receiverId").description("receiverId"),
                        fieldWithPath("[].content").description("content"),
                    )
                )
            )
            .returnResult()
            .responseBody!!

        assertEquals(2, messages.size)
    }

    @Test
    fun `getMessage responses found message`() {
        val senderId = UserId(UUID.randomUUID().toString())
        val receiverId = UserId(UUID.randomUUID().toString())

        val request = fm.giveMeKotlinBuilder<SendMessageRequest>()
            .setExp(SendMessageRequest::senderId, senderId)
            .setExp(SendMessageRequest::receiverId, receiverId)
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
                        fieldWithPath("receiverId").description("receiverId"),
                        fieldWithPath("content").description("content"),
                    )
                )
            )
    }
}
