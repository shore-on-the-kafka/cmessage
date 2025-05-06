package me.chacham.cmessage.auth.domain

data class UserInfo(
    val provider: String,
    val id: String,
    val name: String,
)
