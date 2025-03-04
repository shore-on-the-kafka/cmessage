package me.chacham.cmessage.api.group

import me.chacham.cmessage.group.domain.GroupId
import me.chacham.cmessage.user.domain.UserId

data class AddMembersResponse(val groupId: GroupId, val members: List<UserId>)
