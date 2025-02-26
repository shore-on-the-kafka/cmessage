package me.chacham.cmessage.message.repository

import me.chacham.cmessage.message.domain.Message
import me.chacham.cmessage.message.domain.MessageId
import me.chacham.cmessage.user.domain.UserId

interface MessageRepository {
    suspend fun saveMessage(senderId: UserId, receiverId: UserId, content: String): MessageId
    suspend fun findMessages(senderId: UserId?, receiverId: UserId?): List<Message>
    suspend fun findMessage(messageId: MessageId): Message?
}
