package me.chacham.cmessage.api.viewmodels

import me.chacham.cmessage.user.domain.User
import me.chacham.cmessage.user.domain.UserId

data class UserView(
    val id: UserId,
    val username: String,
) {
    companion object {
        fun of(user: User): UserView {
            return UserView(
                id = user.id,
                username = user.name,
            )
        }
    }
}
