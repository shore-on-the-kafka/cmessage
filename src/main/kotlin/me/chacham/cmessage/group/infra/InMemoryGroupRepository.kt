package me.chacham.cmessage.group.infra

import me.chacham.cmessage.group.domain.Group
import me.chacham.cmessage.group.domain.GroupId
import me.chacham.cmessage.group.repository.GroupRepository
import me.chacham.cmessage.user.domain.UserId
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class InMemoryGroupRepository: GroupRepository {
    private val groups = mutableMapOf<GroupId, Group>()

    override suspend fun saveGroup(name: String, members: List<UserId>): GroupId {
        val groupId = generateGroupId()
        val group = Group(
            id = groupId,
            name = name,
            members = members
        )
        groups[groupId] = group
        return groupId
    }

    override suspend fun findGroup(groupId: GroupId): Group? {
        return groups[groupId]
    }

    override suspend fun findGroupsOfUser(userId: UserId): List<Group> {
        return groups.values.filter { it.members.contains(userId) }
    }

    override suspend fun addMembers(groupId: GroupId, userIds: List<UserId>) {
        // TODO: Add validation for existing members
        groups[groupId]?.let { group ->
            groups[groupId] = group.copy(members = group.members + userIds)
        }
    }

    private fun generateGroupId(): GroupId {
        return GroupId(UUID.randomUUID().toString())
    }
}
