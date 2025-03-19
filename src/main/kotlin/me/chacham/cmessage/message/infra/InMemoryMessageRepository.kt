package me.chacham.cmessage.message.infra

import me.chacham.cmessage.group.domain.GroupId
import me.chacham.cmessage.message.domain.Message
import me.chacham.cmessage.message.domain.MessageId
import me.chacham.cmessage.message.repository.MessageRepository
import me.chacham.cmessage.user.domain.UserId
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class InMemoryMessageRepository : MessageRepository {
    private val messages = mutableMapOf<MessageId, Message>()

    override suspend fun saveMessage(
        senderId: UserId,
        receiverId: UserId?,
        groupId: GroupId?,
        content: String,
    ): MessageId {
        val messageId = generateMessageId()
        val message = Message(
            messageId = messageId,
            senderId = senderId,
            receiverId = receiverId,
            groupId = groupId,
            content = content,
        )
        messages[messageId] = message
        return messageId
    }

    override suspend fun findMessages(senderId: UserId?, receiverId: UserId?): List<Message> {
        return messages.values
            .filter { senderId == null || it.senderId == senderId }
            .filter { receiverId == null || it.receiverId == receiverId }
    }

    override suspend fun findGroupMessages(groupId: GroupId): List<Message> {
        return messages.values
            .filter { it.groupId == groupId }
    }

    override suspend fun findMessage(messageId: MessageId): Message? {
        return messages[messageId]
    }

    private fun generateMessageId(): MessageId {
        return MessageId(UUID.randomUUID().toString())
    }
}
