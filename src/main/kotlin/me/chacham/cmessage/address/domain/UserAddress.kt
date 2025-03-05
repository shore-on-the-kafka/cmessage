package me.chacham.cmessage.address.domain

import com.fasterxml.jackson.annotation.JsonValue
import me.chacham.cmessage.group.domain.GroupId
import me.chacham.cmessage.user.domain.UserId

data class UserAddress(private val userId: UserId) : Address {
    override fun getUserId(): UserId {
        return userId
    }

    override fun getGroupId(): GroupId? {
        return null
    }

    @JsonValue
    fun serialize(): String {
        return userId.id
    }
}
