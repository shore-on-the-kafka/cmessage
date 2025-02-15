package me.chacham.cmessage.message.api

data class SendMessageRequest(val senderId: String, val receiverId: String, val content: String)
