package me.chacham.cmessage.message.infra

import kotlinx.coroutines.runBlocking
import me.chacham.cmessage.user.domain.UserId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.util.*
import kotlin.test.Test

class InMemoryMessageRepositoryTest {
    private val cut = InMemoryMessageRepository()

    @Test
    fun `saveMessage should save message and return message with created messageId`() {
        // given
        val senderId = UserId(UUID.randomUUID().toString())
        val receiverId = UserId(UUID.randomUUID().toString())
        val content = "content"

        // when
        val messageId = runBlocking { cut.saveMessage(senderId, receiverId, null, content) }

        // then
        val message = runBlocking { cut.findMessage(messageId) }
        assertEquals(messageId, message?.messageId)
        assertEquals(senderId, message?.senderId)
        assertEquals(receiverId, message?.receiverId)
        assertEquals(content, message?.content)
    }

    @Test
    fun `findMessages should return messages with matching senderId and receiverId`() {
        // given
        val senderId = generateUserId()
        val receiverId = generateUserId()
        val content = "content"
        val messageId1 = runBlocking { cut.saveMessage(senderId, generateUserId(), null, content) }
        val messageId2 = runBlocking { cut.saveMessage(generateUserId(), receiverId, null, content) }
        val messageId3 = runBlocking { cut.saveMessage(generateUserId(), generateUserId(), null, content) }

        // when
        val messages = runBlocking { cut.findMessages(senderId, receiverId) }

        // then
        assertEquals(2, messages.size)
        assertTrue(messages.any { it.messageId == messageId1 })
        assertTrue(messages.any { it.messageId == messageId2 })
        assertTrue(messages.none { it.messageId == messageId3 })
    }

    @Test
    fun `findMessage should return message with matching messageId`() {
        // given
        val senderId = UserId(UUID.randomUUID().toString())
        val receiverId = UserId(UUID.randomUUID().toString())
        val content = "content"
        val messageId = runBlocking { cut.saveMessage(senderId, receiverId, null, content) }

        // when
        val message = runBlocking { cut.findMessage(messageId) }

        // then
        assertEquals(messageId, message?.messageId)
        assertEquals(senderId, message?.senderId)
        assertEquals(receiverId, message?.receiverId)
        assertEquals(content, message?.content)
    }

    private fun generateUserId(): UserId {
        return UserId(UUID.randomUUID().toString())
    }
}
