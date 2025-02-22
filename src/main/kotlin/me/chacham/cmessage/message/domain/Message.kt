package me.chacham.cmessage.message.domain

data class Message(
    val messageId: MessageId,
    val senderId: String,
    val receiverId: String,
    val content: String,
)
