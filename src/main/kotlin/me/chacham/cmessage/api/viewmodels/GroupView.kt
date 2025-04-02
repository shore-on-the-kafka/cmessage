package me.chacham.cmessage.api.viewmodels

import me.chacham.cmessage.group.domain.Group
import me.chacham.cmessage.group.domain.GroupId

data class GroupView(
    val id: GroupId,
    val name: String,
    val members: List<UserView>,
) {
    companion object {
        fun of(group: Group, members: List<UserView>): GroupView {
            return GroupView(
                id = group.id,
                name = group.name,
                members = members,
            )
        }
    }
}
