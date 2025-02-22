package me.chacham.cmessage.message.infra

import me.chacham.cmessage.message.domain.Message
import me.chacham.cmessage.message.domain.MessageId
import me.chacham.cmessage.message.repository.MessageRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class InMemoryMessageRepository: MessageRepository {
    private val messages = mutableMapOf<MessageId, Message>()

    override suspend fun saveMessage(senderId: String, receiverId: String, content: String): MessageId {
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

    override suspend fun findMessage(messageId: MessageId): Message? {
        synchronized(this) {
            return messages[messageId]
        }
    }

    private fun generateMessageId(): MessageId {
        return MessageId(UUID.randomUUID().toString())
    }
}
