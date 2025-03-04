package me.chacham.cmessage.api.group

import me.chacham.cmessage.user.domain.UserId

data class AddMembersRequest(val users: List<UserId>)
