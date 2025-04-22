package me.chacham.cmessage.api.group

import me.chacham.cmessage.user.domain.UserId

data class CreateGroupRequest(val name: String, val members: List<UserId> = listOf())
