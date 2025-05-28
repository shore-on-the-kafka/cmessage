package me.chacham.cmessage.integrationtest.scenario

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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.wheneverBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList
import org.springframework.web.reactive.function.BodyInserters
import java.util.*

@WebFluxTest(
    MessageController::class,
    GroupController::class
)
@Import(
    AuthController::class, SecurityConfig::class, InMemoryUserRepository::class, JwtService::class,
    MessageService::class, InMemoryMessageRepository::class,
    InMemoryGroupRepository::class
)
class GroupMessageTest {
    private val fm = FixtureMonkey.builder().plugin(KotlinPlugin()).build()

    @MockitoBean
    private lateinit var authService: AuthService

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    fun `users can send and receive group messages`() {
        val user1 = User(UserId(UUID.randomUUID().toString()), "user1")
        val user2 = User(UserId(UUID.randomUUID().toString()), "user2")
        val user3 = User(UserId(UUID.randomUUID().toString()), "user3")
        val token1 = login(user1)
        val token2 = login(user2)

        val groupMembers = listOf(user1.id, user2.id, user3.id)
        val createGroupRequest = fm.giveMeKotlinBuilder<CreateGroupRequest>()
            .setExp(CreateGroupRequest::members, groupMembers.map { it.id })
            .sample()
        val groupId = createGroup(token1, createGroupRequest)

        val messageFromUser1 = fm.giveMeKotlinBuilder<SendMessageRequest>()
            .setExp(SendMessageRequest::groupId, groupId)
            .setNullExp(SendMessageRequest::receiverId)
            .sample()

        webTestClient.post()
            .uri("/api/v1/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer $token1")
            .body(BodyInserters.fromValue(messageFromUser1))
            .exchange()
            .expectStatus().isCreated

        val messageFromUser2 = fm.giveMeKotlinBuilder<SendMessageRequest>()
            .setExp(SendMessageRequest::groupId, groupId)
            .setNullExp(SendMessageRequest::receiverId)
            .sample()

        webTestClient.post()
            .uri("/api/v1/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer $token2")
            .body(BodyInserters.fromValue(messageFromUser2))
            .exchange()
            .expectStatus().isCreated

        val messagesForGroupUser1 = webTestClient.get()
            .uri("/api/v1/messages?groupId={groupId}", groupId.id)
            .header("Authorization", "Bearer $token1") // 그룹 멤버 누구나 조회 가능해야 함
            .exchange()
            .expectStatus().isOk
            .expectBodyList<Message>()
            .returnResult()
            .responseBody

        assertEquals(2, messagesForGroupUser1?.size)
        assertTrue(messagesForGroupUser1?.any { it.content == messageFromUser1.content && it.senderId == user1.id && it.groupId == groupId } ?: false)
        assertTrue(messagesForGroupUser1?.any { it.content == messageFromUser2.content && it.senderId == user2.id && it.groupId == groupId } ?: false)

        val token3 = login(user3)
        val messagesForGroupUser3 = webTestClient.get()
            .uri("/api/v1/messages?groupId={groupId}", groupId.id)
            .header("Authorization", "Bearer $token3")
            .exchange()
            .expectStatus().isOk
            .expectBodyList<Message>()
            .returnResult()
            .responseBody

        assertEquals(2, messagesForGroupUser3?.size)
        assertTrue(messagesForGroupUser3?.any { it.content == messageFromUser1.content && it.senderId == user1.id && it.groupId == groupId } ?: false)
        assertTrue(messagesForGroupUser3?.any { it.content == messageFromUser2.content && it.senderId == user2.id && it.groupId == groupId } ?: false)
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

    private fun createGroup(token: String, request: CreateGroupRequest): GroupId {
        val response = webTestClient.post()
            .uri("/api/v1/groups")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer $token")
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus().isCreated
            .expectBody<CreateGroupResponse>()
            .returnResult()
            .responseBody!!
        return GroupId(response.groupId.id)
    }
}
