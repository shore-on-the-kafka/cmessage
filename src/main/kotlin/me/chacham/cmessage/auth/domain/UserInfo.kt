package me.chacham.cmessage.auth.domain

import me.chacham.cmessage.user.domain.UserId

data class UserInfo(
    val provider: String,
    val id: UserId,
    val name: String,
)
