package me.chacham.cmessage.api.message

import me.chacham.cmessage.user.domain.UserId

data class SendMessageRequest(val senderId: UserId, val receiverId: UserId, val content: String)
