package me.chacham.cmessage.message.infra

import me.chacham.cmessage.address.domain.Address
import me.chacham.cmessage.address.domain.GroupAddress
import me.chacham.cmessage.address.domain.UserAddress
import me.chacham.cmessage.address.domain.UserAtGroupAddress
import me.chacham.cmessage.message.domain.Message
import me.chacham.cmessage.message.domain.MessageId
import me.chacham.cmessage.message.repository.MessageRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class InMemoryMessageRepository : MessageRepository {
    private val messages = mutableMapOf<MessageId, Message>()

    override suspend fun saveMessage(senderAddress: Address, receiverAddress: Address, content: String): MessageId {
        val messageId = generateMessageId()
        val message = Message(
            messageId = messageId,
            senderAddress = senderAddress,
            receiverAddress = receiverAddress,
            content = content,
        )
        messages[messageId] = message
        return messageId
    }

    override suspend fun findMessages(senderAddress: Address?, receiverAddress: Address?): List<Message> {
        return messages.values
            .filter { matchAddress(senderAddress, it.senderAddress) }
            .filter { matchAddress(receiverAddress, it.receiverAddress) }
    }

    private fun matchAddress(address: Address?, messageAddress: Address): Boolean {
        return when (address) {
            null -> true
            is UserAddress -> {
                address.getUserId() == messageAddress.getUserId()
            }

            is GroupAddress -> {
                address.getGroupId() == messageAddress.getGroupId()
            }

            is UserAtGroupAddress -> {
                address.getUserId() == messageAddress.getUserId() && address.getGroupId() == messageAddress.getGroupId()
            }
        }
    }

    override suspend fun findMessage(messageId: MessageId): Message? {
        return messages[messageId]
    }

    private fun generateMessageId(): MessageId {
        return MessageId(UUID.randomUUID().toString())
    }
}
