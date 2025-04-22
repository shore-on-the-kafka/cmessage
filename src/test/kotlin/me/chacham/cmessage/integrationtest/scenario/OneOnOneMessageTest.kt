package me.chacham.cmessage.integrationtest.scenario

import com.navercorp.fixturemonkey.FixtureMonkey
import com.navercorp.fixturemonkey.kotlin.KotlinPlugin
import com.navercorp.fixturemonkey.kotlin.giveMeKotlinBuilder
import me.chacham.cmessage.api.auth.*
import me.chacham.cmessage.api.message.MessageController
import me.chacham.cmessage.api.message.SendMessageRequest
import me.chacham.cmessage.common.config.JwtService
import me.chacham.cmessage.common.config.SecurityConfig
import me.chacham.cmessage.group.infra.InMemoryGroupRepository
import me.chacham.cmessage.message.domain.Message
import me.chacham.cmessage.message.infra.InMemoryMessageRepository
import me.chacham.cmessage.message.service.MessageService
import me.chacham.cmessage.user.domain.User
import me.chacham.cmessage.user.domain.UserId
import me.chacham.cmessage.user.infra.InMemoryUserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList
import org.springframework.web.reactive.function.BodyInserters

@WebFluxTest(
    MessageController::class
)
@Import(
    AuthController::class, SecurityConfig::class, InMemoryUserRepository::class, JwtService::class,
    MessageService::class, InMemoryMessageRepository::class, InMemoryGroupRepository::class
)
class OneOnOneMessageTest {

    private val fm = FixtureMonkey.builder().plugin(KotlinPlugin()).build()

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    fun `users can send and receive one-on-one messages`() {
        val user1 = register()
        val user2 = register()
        val token1 = login(user1)
        val token2 = login(user2)

        val messageFromUser1 = fm.giveMeKotlinBuilder<SendMessageRequest>()
            .setExp(SendMessageRequest::receiverId, user2.id)
            .setNullExp(SendMessageRequest::groupId)
            .sample()

        webTestClient.post()
            .uri("/api/v1/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer $token1")
            .body(BodyInserters.fromValue(messageFromUser1))
            .exchange()
            .expectStatus().isCreated

        val messageFromUser2 = fm.giveMeKotlinBuilder<SendMessageRequest>()
            .setExp(SendMessageRequest::receiverId, user1.id)
            .setNullExp(SendMessageRequest::groupId)
            .sample()

        webTestClient.post()
            .uri("/api/v1/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer $token2")
            .body(BodyInserters.fromValue(messageFromUser2))
            .exchange()
            .expectStatus().isCreated

        val messagesForUser1 = webTestClient.get()
            .uri("/api/v1/messages?userIdPair={user1Id}+{user2Id}", user1.id.id, user2.id.id)
            .header("Authorization", "Bearer $token1")
            .exchange()
            .expectStatus().isOk
            .expectBodyList<Message>()
            .returnResult()
            .responseBody

        assertEquals(2, messagesForUser1?.size)
        assertEquals(user1.id, messagesForUser1?.find { it.content == messageFromUser1.content }?.senderId)
        assertEquals(user2.id, messagesForUser1?.find { it.content == messageFromUser1.content }?.receiverId)
        assertEquals(user2.id, messagesForUser1?.find { it.content == messageFromUser2.content }?.senderId)
        assertEquals(user1.id, messagesForUser1?.find { it.content == messageFromUser2.content }?.receiverId)


        val messagesForUser2 = webTestClient.get()
            .uri("/api/v1/messages?userIdPair={user2Id}+{user1Id}", user2.id.id, user1.id.id)
            .header("Authorization", "Bearer $token2")
            .exchange()
            .expectStatus().isOk
            .expectBodyList<Message>()
            .returnResult()
            .responseBody

        assertEquals(2, messagesForUser2?.size)
        assertEquals(user1.id, messagesForUser2?.find { it.content == messageFromUser1.content }?.senderId)
        assertEquals(user2.id, messagesForUser2?.find { it.content == messageFromUser1.content }?.receiverId)
        assertEquals(user2.id, messagesForUser2?.find { it.content == messageFromUser2.content }?.senderId)
        assertEquals(user1.id, messagesForUser2?.find { it.content == messageFromUser2.content }?.receiverId)
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
            password = registerRequest.password, // 실제 비밀번호 저장 (테스트용)
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
