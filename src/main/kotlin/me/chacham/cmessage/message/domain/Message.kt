package me.chacham.cmessage.message.domain

import me.chacham.cmessage.group.domain.GroupId
import me.chacham.cmessage.user.domain.UserId

data class Message(
    val messageId: MessageId,
    val senderId: UserId,
    val receiverId: UserId?,
    val groupId: GroupId?,
    val content: String,
)
