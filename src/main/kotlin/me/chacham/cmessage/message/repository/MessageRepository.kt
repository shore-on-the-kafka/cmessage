package me.chacham.cmessage.message.repository

import me.chacham.cmessage.address.domain.Address
import me.chacham.cmessage.message.domain.Message
import me.chacham.cmessage.message.domain.MessageId

interface MessageRepository {
    suspend fun saveMessage(senderAddress: Address, receiverAddress: Address, content: String): MessageId
    suspend fun findMessages(senderAddress: Address?, receiverAddress: Address?): List<Message>
    suspend fun findMessage(messageId: MessageId): Message?
}
