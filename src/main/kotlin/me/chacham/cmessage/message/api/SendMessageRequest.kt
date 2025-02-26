package me.chacham.cmessage.message.api

import me.chacham.cmessage.user.domain.UserId

data class SendMessageRequest(val senderId: UserId, val receiverId: UserId, val content: String)
