package me.chacham.cmessage.user.infra

import me.chacham.cmessage.user.domain.User
import me.chacham.cmessage.user.domain.UserId
import me.chacham.cmessage.user.repository.UserRepository
import org.springframework.stereotype.Repository
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Repository
class InMemoryUserRepository : UserRepository {
    private val users = ConcurrentHashMap<UserId, User>()

    override suspend fun saveUser(username: String, password: String): User {
        val userId = UserId(UUID.randomUUID().toString())
        val user = User(id = userId, username = username, password = password)
        users[userId] = user
        return user
    }

    override suspend fun findUserById(id: UserId): User? {
        return users[id]
    }

    override suspend fun findUserByUsername(username: String): User? {
        return users.values.find { it.username == username }
    }
}
