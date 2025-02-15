package me.chacham.cmessage.message.integrationtest

import me.chacham.cmessage.message.api.MessageController
import me.chacham.cmessage.message.api.SendMessageRequest
import me.chacham.cmessage.message.domain.Message
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
import org.springframework.web.reactive.function.BodyInserters
import kotlin.test.Test

@WebFluxTest(MessageController::class)
@AutoConfigureRestDocs
class MessageDocumentTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    fun `sendMessage responses Created with created resource Location header`() {
        val request = SendMessageRequest("senderId", "receiverId", "content")
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
                    )
                )
            )
    }

    @Test
    fun `getMessages responses message list`() {
        webTestClient.get()
            .uri("/api/v1/messages?senderId=senderId&receiverId=receiverId")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .consumeWith(
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
    }

    @Test
    fun `getMessage responses found message`() {
        val messageId = "testId"

        webTestClient.get()
            .uri("/api/v1/messages/{messageId}", messageId)
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
