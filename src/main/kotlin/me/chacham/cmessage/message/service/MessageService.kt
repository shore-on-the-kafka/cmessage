package me.chacham.cmessage.message.service

import me.chacham.cmessage.group.domain.GroupId
import me.chacham.cmessage.group.repository.GroupRepository
import me.chacham.cmessage.message.domain.Message
import me.chacham.cmessage.message.domain.MessageId
import me.chacham.cmessage.message.repository.MessageRepository
import me.chacham.cmessage.user.domain.UserId
import org.springframework.stereotype.Service

@Service
class MessageService(private val messageRepository: MessageRepository, private val groupRepository: GroupRepository) {
    suspend fun sendUserMessage(senderId: UserId, receiverId: UserId, content: String): MessageId {
        return messageRepository.saveMessage(senderId, receiverId, null, content)
    }

    suspend fun sendGroupMessage(senderId: UserId, groupId: GroupId, content: String): MessageId {
        return messageRepository.saveMessage(senderId, null, groupId, content)
    }

    suspend fun findSenderMessages(senderId: UserId): List<Message> {
        return messageRepository.findMessages(senderId, null)
    }

    suspend fun findReceiverMessages(receiverId: UserId): List<Message> {
        val groups = groupRepository.findGroupsOfUser(receiverId)
        return messageRepository.findMessages(null, receiverId) + groups.flatMap { group ->
            messageRepository.findGroupMessages(group.id)
        }
    }

    suspend fun findSenderReceiverMessages(senderId: UserId, receiverId: UserId): List<Message> {
        return messageRepository.findMessages(senderId, receiverId)
    }

    suspend fun findGroupMessages(groupId: GroupId): List<Message> {
        return messageRepository.findGroupMessages(groupId)
    }

    suspend fun findMessage(messageId: MessageId): Message? {
        return messageRepository.findMessage(messageId)
    }
}
