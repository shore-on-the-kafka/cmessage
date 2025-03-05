package me.chacham.cmessage.address.domain

import com.fasterxml.jackson.annotation.JsonValue
import me.chacham.cmessage.group.domain.GroupId
import me.chacham.cmessage.user.domain.UserId

data class UserAtGroupAddress(private val userId: UserId, private val groupId: GroupId) : Address {
    override fun getUserId(): UserId {
        return userId
    }

    override fun getGroupId(): GroupId {
        return groupId
    }

    @JsonValue
    fun serialize(): String {
        return "${userId.id}@${groupId.id}"
    }
}
