package me.chacham.cmessage.user.repository

import me.chacham.cmessage.user.domain.User
import me.chacham.cmessage.user.domain.UserId

interface UserRepository {
    suspend fun saveUser(username: String, password: String): User
    suspend fun findUserById(id: UserId): User?
    suspend fun findUserByUsername(username: String): User?
}
