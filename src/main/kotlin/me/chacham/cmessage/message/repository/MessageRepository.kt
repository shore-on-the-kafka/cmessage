package me.chacham.cmessage.message.repository

import me.chacham.cmessage.message.domain.Message

interface MessageRepository {
    suspend fun saveMessage(senderId: String, receiverId: String, content: String): String
    suspend fun findMessages(senderId: String?, receiverId: String?): List<Message>
    suspend fun findMessage(messageId: String): Message?
}
