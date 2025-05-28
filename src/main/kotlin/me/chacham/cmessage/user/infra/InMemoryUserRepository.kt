package me.chacham.cmessage.user.infra

import me.chacham.cmessage.user.domain.User
import me.chacham.cmessage.user.domain.UserId
import me.chacham.cmessage.user.repository.UserRepository
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
class InMemoryUserRepository : UserRepository {
    private val users = ConcurrentHashMap<UserId, User>()

    override suspend fun saveUser(id: UserId, name: String): User {
        val user = User(id = id, name = name)
        users[id] = user
        return user
    }

    override suspend fun find(id: UserId): User? {
        return users[id]
    }

    override suspend fun findUserByUsername(username: String): User? {
        return users.values.find { it.name == username }
    }
}
