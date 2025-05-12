package me.chacham.cmessage.user.repository

import me.chacham.cmessage.user.domain.User
import me.chacham.cmessage.user.domain.UserId

interface UserRepository {
    suspend fun saveUser(id: UserId, name: String): User
    suspend fun find(id: UserId): User?
    suspend fun findUserByUsername(username: String): User?
}
