package me.chacham.cmessage.group.repository

import me.chacham.cmessage.group.domain.Group
import me.chacham.cmessage.group.domain.GroupId
import me.chacham.cmessage.user.domain.UserId

interface GroupRepository {
    suspend fun saveGroup(name: String, members: List<UserId>): GroupId
    suspend fun findGroup(groupId: GroupId): Group?
    suspend fun findGroupsOfUser(userId: UserId): List<Group>
    suspend fun addMembers(groupId: GroupId, userIds: List<UserId>)
}
