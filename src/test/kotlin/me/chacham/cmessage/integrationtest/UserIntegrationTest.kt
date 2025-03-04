package me.chacham.cmessage.integrationtest

import com.navercorp.fixturemonkey.FixtureMonkey
import com.navercorp.fixturemonkey.kotlin.KotlinPlugin
import com.navercorp.fixturemonkey.kotlin.giveMeKotlinBuilder
import me.chacham.cmessage.api.group.CreateGroupRequest
import me.chacham.cmessage.api.group.GroupController
import me.chacham.cmessage.api.user.UserController
import me.chacham.cmessage.group.repository.GroupRepository
import me.chacham.cmessage.user.domain.UserId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.MediaType
import org.springframework.restdocs.operation.preprocess.Preprocessors.*
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import kotlin.test.Test

@WebFluxTest(UserController::class, GroupController::class, GroupRepository::class)
@AutoConfigureRestDocs
class UserIntegrationTest {
    private val fm = FixtureMonkey.builder().plugin(KotlinPlugin()).build()

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    fun `getMyGroups responses 200 OK with my groups as body`() {
        // Given
        val userId = UserId("user-id")
        val createGroupRequests = fm.giveMeKotlinBuilder<CreateGroupRequest>().sampleList(2)
        for (request in createGroupRequests) {
            webTestClient.post()
                .uri("/api/v1/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", userId.id)
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isCreated
        }

        // When, Then
        webTestClient.get()
            .uri("/api/v1/users/me/groups")
            .header("X-User-Id", userId.id)
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
}
