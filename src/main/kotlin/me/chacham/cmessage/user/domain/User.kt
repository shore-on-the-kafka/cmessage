package me.chacham.cmessage.user.domain

data class User(
    val id: UserId,
    val username: String,
    val password: String,
)
