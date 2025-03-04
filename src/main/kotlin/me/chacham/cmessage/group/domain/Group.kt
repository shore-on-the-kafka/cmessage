package me.chacham.cmessage.group.domain

import me.chacham.cmessage.user.domain.UserId

data class Group(val id: GroupId, val name: String, val members: List<UserId>)
