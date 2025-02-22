package me.chacham.cmessage.message.repository

import me.chacham.cmessage.message.domain.Message
import me.chacham.cmessage.message.domain.MessageId

interface MessageRepository {
    suspend fun saveMessage(senderId: String, receiverId: String, content: String): MessageId
    suspend fun findMessages(senderId: String?, receiverId: String?): List<Message>
    suspend fun findMessage(messageId: MessageId): Message?
}
