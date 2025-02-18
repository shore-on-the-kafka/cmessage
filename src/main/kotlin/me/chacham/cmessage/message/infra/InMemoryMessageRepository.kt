package me.chacham.cmessage.message.infra

import me.chacham.cmessage.message.domain.Message
import me.chacham.cmessage.message.repository.MessageRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class InMemoryMessageRepository: MessageRepository {
    private val messages = mutableMapOf<String, Message>()

    override suspend fun saveMessage(senderId: String, receiverId: String, content: String): String {
        synchronized(this) {
            val messageId = generateMessageId()
            val message = Message(
                messageId = messageId,
                senderId = senderId,
                receiverId = receiverId,
                content = content,
            )
            messages[messageId] = message
            return messageId
        }
    }

    override suspend fun findMessages(senderId: String?, receiverId: String?): List<Message> {
        synchronized(this) {
            return messages.values
                .filter { senderId == null || it.senderId == senderId }
                .filter { receiverId == null || it.receiverId == receiverId }
        }
    }

    override suspend fun findMessage(messageId: String): Message? {
        synchronized(this) {
            return messages[messageId]
        }
    }

    private fun generateMessageId(): String {
        return UUID.randomUUID().toString()
    }
}
