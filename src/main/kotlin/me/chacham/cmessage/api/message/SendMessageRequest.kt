package me.chacham.cmessage.api.message

import me.chacham.cmessage.group.domain.GroupId
import me.chacham.cmessage.user.domain.UserId

data class SendMessageRequest(
    val receiverId: UserId?,
    val groupId: GroupId?,
    val content: String,
)
