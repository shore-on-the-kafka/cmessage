package me.chacham.cmessage.message.domain

import me.chacham.cmessage.address.domain.Address

data class Message(
    val messageId: MessageId,
    val senderAddress: Address,
    val receiverAddress: Address,
    val content: String,
)
