package me.chacham.cmessage.message.domain

data class Message(
    val messageId: String,
    val senderId: String,
    val receiverId: String,
    val content: String,
)
